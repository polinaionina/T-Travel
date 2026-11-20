package ru.ttravel.orchestrator.booking;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.ttravel.orchestrator.domain.data.ComponentAttempt;
import ru.ttravel.orchestrator.domain.data.OrderComponent;
import ru.ttravel.orchestrator.domain.data.TripOrder;
import ru.ttravel.orchestrator.domain.exceptions.InvalidRequestException;
import ru.ttravel.orchestrator.domain.exceptions.OrderNotFoundException;
import ru.ttravel.orchestrator.domain.exceptions.OrchestratorException;
import ru.ttravel.orchestrator.domain.models.AttemptOperation;
import ru.ttravel.orchestrator.domain.models.AttemptResult;
import ru.ttravel.orchestrator.domain.models.ComponentStatus;
import ru.ttravel.orchestrator.domain.models.EventType;
import ru.ttravel.orchestrator.domain.models.OrderStatus;
import ru.ttravel.orchestrator.domain.repositories.ComponentAttemptRepository;
import ru.ttravel.orchestrator.domain.repositories.OrderRepository;
import ru.ttravel.orchestrator.order.OrderTransitionService;

import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CompensationService {

    private final OrderRepository orderRepository;
    private final ComponentAttemptRepository attemptRepository;
    private final ComponentProcessor componentProcessor;
    private final OrderTransitionService transitionService;

    private static final Logger log = LoggerFactory.getLogger(CompensationService.class);

    public CompensationService(
            OrderRepository orderRepository,
            ComponentAttemptRepository attemptRepository,
            ComponentProcessor componentProcessor,
            OrderTransitionService transitionService
    ) {
        this.orderRepository = orderRepository;
        this.attemptRepository = attemptRepository;
        this.componentProcessor = componentProcessor;
        this.transitionService = transitionService;
    }

    /**
     * Запускает компенсацию заказа.
     *
     * Переводит заказ в COMPENSATING и начинает отмену
     * последнего активного компонента.
     *
     * @param orderId идентификатор заказа
     * @param userId идентификатор пользователя
     * @return попытка отмены первого компонента или null,
     * если отмена пока не может быть запущена
     * @throws OrchestratorException если заказ не найден
     * или переход состояния недопустим
     */
    @Transactional
    public ComponentAttempt startCompensation(Long orderId, Long userId)
            throws OrchestratorException {

        TripOrder order = getOrder(orderId, userId);

        log.info("Starting compensation: orderId={}, userId={}, status={}",
                orderId,
                userId,
                order.getStatus()
        );

        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.debug("Order is already cancelled: orderId={}", orderId);
            return null;
        }

        if (order.getStatus() == OrderStatus.COMPENSATING) {
            log.debug("Compensation already in progress: orderId={}", orderId);
            return startNextCancellation(order);
        }

        if (order.getStatus() == OrderStatus.COMPENSATION_FAILED) {
            transitionService.transition(
                    order,
                    OrderStatus.COMPENSATING,
                    EventType.COMPENSATION_STARTED,
                    "Повторный запуск компенсации"
            );
        } else {
            transitionService.transition(
                    order,
                    OrderStatus.COMPENSATING,
                    EventType.COMPENSATION_STARTED,
                    "Начата компенсация заказа"
            );
        }

        return startNextCancellation(order);
    }

    /**
     * Обрабатывает результат отмены компонента.
     *
     * При успехе запускает отмену следующего компонента.
     * При ошибке переводит заказ в COMPENSATION_FAILED.
     * При таймауте ожидает разрешения UNKNOWN.
     *
     * @param orderId идентификатор заказа
     * @param userId идентификатор пользователя
     * @param componentId идентификатор компонента
     * @param attempt попытка отмены
     * @param result результат операции
     * @param reason пояснение результата
     * @return следующая попытка отмены или null
     * @throws OrchestratorException если обработка невозможна
     */
    @Transactional
    public ComponentAttempt handleCancellationResult(
            Long orderId,
            Long userId,
            Long componentId,
            ComponentAttempt attempt,
            AttemptResult result,
            String reason
    ) throws OrchestratorException {

        TripOrder order = getOrder(orderId, userId);

        log.info("Handling cancellation result: orderId={}, componentId={}, result={}",
                orderId,
                componentId,
                result
        );

        if (order.getStatus() != OrderStatus.COMPENSATING) {
            throw new InvalidRequestException(
                    "Заказ не находится в состоянии COMPENSATING"
            );
        }

        boolean belongsToOrder = order.activeComponents().stream()
                .anyMatch(component ->
                        component.getId().equals(componentId)
                );

        if (!belongsToOrder) {
            throw new InvalidRequestException(
                    "Компонент не принадлежит указанному заказу"
            );
        }

        componentProcessor.completeCancel(
                componentId,
                attempt,
                result,
                reason
        );

        if (result == AttemptResult.FAILED) {
            log.error("Component cancellation failed: orderId={}, componentId={}, reason={}",
                    orderId,
                    componentId,
                    reason
            );
            transitionService.transition(
                    order,
                    OrderStatus.COMPENSATION_FAILED,
                    EventType.COMPENSATION_FAILED,
                    "Не удалось отменить компонент " + componentId
            );

            return null;
        }

        if (result == AttemptResult.TIMEOUT) {
            log.warn("Component cancellation timed out: orderId={}, componentId={}",
                    orderId,
                    componentId
            );
            return null;
        }

        order = getOrder(orderId, userId);

        return startNextCancellation(order);
    }

    /**
     * Находит следующий компонент для отмены.
     *
     * Компоненты обрабатываются в обратном порядке stepNo.
     */
    private ComponentAttempt startNextCancellation(TripOrder order)
            throws OrchestratorException {

        List<OrderComponent> components = order.activeComponents()
                .stream()
                .sorted(Comparator.comparingLong(
                        OrderComponent::getStepNo
                ).reversed())
                .toList();

        for (OrderComponent component : components) {

            ComponentStatus status = component.getStatus();

            if (status == ComponentStatus.CANCELLED
                    || status == ComponentStatus.FAILED
                    || status == ComponentStatus.PENDING) {
                continue;
            }

            if (status == ComponentStatus.RESERVED
                    || status == ComponentStatus.CONFIRMED) {

                if (hasPendingCancellation(component.getId())) {
                    log.debug("Cancellation already pending: orderId={}, componentId={}",
                            order.getId(),
                            component.getId()
                    );
                    return null;
                }

                log.info("Starting component cancellation: orderId={}, componentId={}, stepNo={}, status={}",
                        order.getId(),
                        component.getId(),
                        component.getStepNo(),
                        status
                );

                return componentProcessor.startCancel(component.getId());
            }

            log.warn("Compensation paused: orderId={}, componentId={}, status={}",
                    order.getId(),
                    component.getId(),
                    status
            );
            return null;
        }

        log.info("All component cancellations completed: orderId={}",
                order.getId()
        );
        transitionService.transition(
                order,
                OrderStatus.CANCELLED,
                EventType.ORDER_CANCELLED,
                "Компенсация всех компонентов завершена"
        );

        return null;
    }

    /**
     * Проверяет наличие незавершённой попытки отмены.
     */
    private boolean hasPendingCancellation(Long componentId) {
        return attemptRepository.existsByComponentIdAndOperationAndResultIsNull(
                componentId,
                AttemptOperation.CANCEL
        );
    }

    /**
     * Загружает заказ и проверяет его владельца.
     */
    private TripOrder getOrder(Long orderId, Long userId)
            throws OrderNotFoundException {

        TripOrder order = orderRepository.findWithComponentsById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getUserId().equals(userId)) {
            throw new OrderNotFoundException(orderId);
        }

        return order;
    }
}
