package ru.ttravel.orchestrator.booking;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.ttravel.orchestrator.domain.data.ComponentAttempt;
import ru.ttravel.orchestrator.domain.data.OrderComponent;
import ru.ttravel.orchestrator.domain.data.TripOrder;
import ru.ttravel.orchestrator.domain.exceptions.InvalidRequestException;
import ru.ttravel.orchestrator.domain.exceptions.OrchestratorException;
import ru.ttravel.orchestrator.domain.models.AttemptOperation;
import ru.ttravel.orchestrator.domain.models.AttemptResult;
import ru.ttravel.orchestrator.domain.models.ComponentStatus;
import ru.ttravel.orchestrator.domain.models.ComponentType;
import ru.ttravel.orchestrator.domain.models.EventType;
import ru.ttravel.orchestrator.domain.models.OrderStatus;
import ru.ttravel.orchestrator.domain.repositories.ComponentAttemptRepository;
import ru.ttravel.orchestrator.order.OrderService;
import ru.ttravel.orchestrator.order.OrderTransitionService;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class BookingService {

    private final OrderService orderService;
    private final OrderTransitionService transitionService;
    private final ComponentProcessor componentProcessor;
    private final ComponentAttemptRepository attemptRepository;

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    public BookingService(
            OrderService orderService,
            OrderTransitionService transitionService,
            ComponentProcessor componentProcessor,
            ComponentAttemptRepository attemptRepository
    ) {
        this.orderService = orderService;
        this.transitionService = transitionService;
        this.componentProcessor = componentProcessor;
        this.attemptRepository = attemptRepository;
    }

    /**
     * Запускает процесс подтверждения заказа.
     *
     * Проверяет обязательные компоненты, переводит заказ
     * в CONFIRMING и создаёт попытку подтверждения первого компонента.
     *
     * @param orderId идентификатор заказа
     * @param userId идентификатор пользователя
     * @return попытка подтверждения первого компонента
     * @throws OrchestratorException если заказ или компоненты
     * не соответствуют требованиям
     */
    @Transactional
    public ComponentAttempt startBooking(Long orderId, Long userId)
            throws OrchestratorException {

        TripOrder order = orderService.getOrder(orderId, userId);

        log.info("Starting booking: orderId={}, userId={}, status={}",
                orderId,
                userId,
                order.getStatus()
        );

        validateComponents(order);

        OrderStatus status = order.getStatus();

        if (status != OrderStatus.DRAFT && status != OrderStatus.ACTION_REQUIRED) {
            log.warn("Cannot start booking: orderId={}, status={}",
                orderId,
                status
            );
            throw new InvalidRequestException(
                "Нельзя запустить подтверждение заказа в статусе " + status
            );
        }

        transitionService.transition(
                order,
                OrderStatus.CONFIRMING,
                EventType.STATUS_CHANGED,
                "Начато подтверждение компонентов заказа"
        );

        return startNextConfirmation(order);
    }

    /**
     * Обрабатывает результат подтверждения компонента.
     *
     * При успехе запускает подтверждение следующего компонента.
     * При ошибке переводит заказ в ACTION_REQUIRED.
     * При таймауте оставляет заказ в CONFIRMING до разрешения UNKNOWN.
     *
     * @param orderId идентификатор заказа
     * @param userId идентификатор пользователя
     * @param componentId идентификатор компонента
     * @param attempt завершённая попытка подтверждения
     * @param result результат операции
     * @param reason пояснение результата
     * @return следующая попытка или null, если дальнейшее
     * подтверждение пока не запускается
     * @throws OrchestratorException если обработка невозможна
     */
    @Transactional
    public ComponentAttempt handleConfirmationResult(
            Long orderId,
            Long userId,
            Long componentId,
            ComponentAttempt attempt,
            AttemptResult result,
            String reason
    ) throws OrchestratorException {

        TripOrder order = orderService.getOrder(orderId, userId);

        log.info("Handling confirmation result: orderId={}, componentId={}, result={}",
                orderId,
                componentId,
                result
        );

        boolean belongsToOrder = order.activeComponents().stream()
                .anyMatch(component -> component.getId().equals(componentId));

        if (!belongsToOrder) {
            throw new InvalidRequestException(
                    "Компонент не принадлежит указанному заказу"
            );
        }

        if (order.getStatus() != OrderStatus.CONFIRMING) {
            throw new InvalidRequestException(
                    "Заказ не находится в состоянии CONFIRMING"
            );
        }

        componentProcessor.completeConfirm(
                componentId,
                attempt,
                result,
                reason
        );

        if (result == AttemptResult.FAILED) {
            log.warn("Component confirmation failed: orderId={}, componentId={}, reason={}",
                    orderId,
                    componentId,
                    reason
            );
            transitionService.transition(
                    order,
                    OrderStatus.ACTION_REQUIRED,
                    EventType.ACTION_REQUIRED,
                    "Не удалось подтвердить компонент " + componentId
            );

            return null;
        }

        if (result == AttemptResult.TIMEOUT) {
            log.warn("Component confirmation timed out: orderId={}, componentId={}",
                    orderId,
                    componentId
            );
            return null;
        }

        order = orderService.getOrder(orderId, userId);

        boolean allConfirmed = order.activeComponents().stream()
                .allMatch(component ->
                        component.getStatus() == ComponentStatus.CONFIRMED
                );

        if (allConfirmed) {
            log.info("All components confirmed: orderId={}",
                    orderId
            );
            transitionService.transition(
                    order,
                    OrderStatus.PAYMENT_PENDING,
                    EventType.STATUS_CHANGED,
                    "Все компоненты подтверждены, ожидается оплата"
             );

            return null;
        }

        return startNextConfirmation(order);
    }

    /**
     * Проверяет наличие всех обязательных компонентов
     * и готовность выбранных компонентов к подтверждению.
     */
    private void validateComponents(TripOrder order)
            throws InvalidRequestException {

        List<OrderComponent> components = order.activeComponents();

        Set<ComponentType> selectedTypes = components.stream()
                .map(OrderComponent::getType)
                .collect(java.util.stream.Collectors.toSet());

        if (!selectedTypes.containsAll(ComponentType.requiredTypes())) {
            log.warn("Booking validation failed: required components are missing, orderId={}",
                order.getId()
            );
            throw new InvalidRequestException(
                    "Не выбраны все обязательные компоненты заказа"
            );
        }

        boolean allReady = components.stream()
                .allMatch(component ->
                        component.getStatus() == ComponentStatus.RESERVED
                                || component.getStatus() == ComponentStatus.CONFIRMED
                );

        if (!allReady) {
                log.warn("Booking validation failed: components are not ready, orderId={}",
                        order.getId());
                throw new InvalidRequestException(
                        "Все выбранные компоненты должны быть зарезервированы или подтверждены"
                );
        }
    }

    /**
     * Находит следующий компонент и запускает его подтверждение.
     */
    private ComponentAttempt startNextConfirmation(TripOrder order)
            throws OrchestratorException {

        for (OrderComponent component : order.activeComponents().stream()
                .sorted(java.util.Comparator.comparingLong(OrderComponent::getStepNo))
                .toList()) {

            if (hasPendingConfirmation(component.getId())) {
                log.debug("Confirmation already pending: orderId={}, componentId={}",
                        order.getId(),
                        component.getId()
                );
                return null;
            }

            if (hasPendingConfirmation(component.getId())) {
                continue;
            }

            log.info("Starting component confirmation: orderId={}, componentId={}, componentType={}",
                order.getId(),
                component.getId(),
                component.getType()
            );

            return componentProcessor.startConfirm(component.getId());
        }

        return null;
    }

    /**
     * Проверяет, существует ли незавершённая попытка подтверждения.
     */
    private boolean hasPendingConfirmation(Long componentId) {
        return attemptRepository.existsByComponentIdAndOperationAndResultIsNull(
                componentId,
                AttemptOperation.CONFIRM
        );
    }
}
