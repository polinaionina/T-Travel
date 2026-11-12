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
import ru.ttravel.orchestrator.events.EventService;
import ru.ttravel.orchestrator.order.OrderService;
import ru.ttravel.orchestrator.order.OrderTransitionService;

import java.util.List;
import java.util.Set;

@Service
public class BookingService {

    private final OrderService orderService;
    private final OrderTransitionService transitionService;
    private final ComponentProcessor componentProcessor;
    private final ComponentAttemptRepository attemptRepository;

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

        validateComponents(order);

        OrderStatus status = order.getStatus();

        if (status != OrderStatus.DRAFT
                && status != OrderStatus.ACTION_REQUIRED) {
            throw new InvalidRequestException(
                    "Нельзя запустить подтверждение заказа в статусе " + status
            );
        }

        transitionService.transition(
                order,
                OrderStatus.SUBMITTED,
                EventType.ORDER_SUBMITTED,
                "Заказ отправлен на подтверждение"
        );

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
            transitionService.transition(
                    order,
                    OrderStatus.ACTION_REQUIRED,
                    EventType.ACTION_REQUIRED,
                    "Не удалось подтвердить компонент " + componentId
            );

            return null;
        }

        if (result == AttemptResult.TIMEOUT) {
            return null;
        }

        order = orderService.getOrder(orderId, userId);

        boolean allConfirmed = order.activeComponents().stream()
                .allMatch(component ->
                        component.getStatus() == ComponentStatus.CONFIRMED
                );

        if (allConfirmed) {
            transitionService.transition(
                    order,
                    OrderStatus.CONFIRMED,
                    EventType.ORDER_CONFIRMED,
                    "Все компоненты заказа подтверждены"
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
            throw new InvalidRequestException(
                    "Не выбраны все обязательные компоненты заказа"
            );
        }

        boolean allReserved = components.stream()
                .allMatch(component ->
                        component.getStatus() == ComponentStatus.RESERVED
                );

        if (!allReserved) {
            throw new InvalidRequestException(
                    "Все выбранные компоненты должны быть зарезервированы"
            );
        }
    }

    /**
     * Находит следующий компонент и запускает его подтверждение.
     */
    private ComponentAttempt startNextConfirmation(TripOrder order)
            throws OrchestratorException {

        for (OrderComponent component : order.activeComponents()) {

            if (component.getStatus() != ComponentStatus.RESERVED) {
                continue;
            }

            if (hasPendingConfirmation(component.getId())) {
                continue;
            }

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
