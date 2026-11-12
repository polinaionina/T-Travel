package ru.ttravel.orchestrator.booking;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.ttravel.orchestrator.booking.models.UnknownResolution;
import ru.ttravel.orchestrator.domain.data.ComponentAttempt;
import ru.ttravel.orchestrator.domain.data.OrderComponent;
import ru.ttravel.orchestrator.domain.exceptions.ComponentNotFoundException;
import ru.ttravel.orchestrator.domain.exceptions.InvalidRequestException;
import ru.ttravel.orchestrator.domain.exceptions.OrchestratorException;
import ru.ttravel.orchestrator.domain.models.AttemptOperation;
import ru.ttravel.orchestrator.domain.models.AttemptResult;
import ru.ttravel.orchestrator.domain.models.ComponentStatus;
import ru.ttravel.orchestrator.domain.models.EventType;
import ru.ttravel.orchestrator.domain.repositories.ComponentAttemptRepository;
import ru.ttravel.orchestrator.domain.repositories.OrderComponentRepository;
import ru.ttravel.orchestrator.domain.state.ComponentStateMachine;
import ru.ttravel.orchestrator.events.EventService;

@Service
public class ComponentProcessor {

    private final OrderComponentRepository componentRepository;
    private final ComponentAttemptRepository attemptRepository;
    private final ComponentStateMachine stateMachine;
    private final EventService eventService;
    private final UnknownResolver unknownResolver;

    public ComponentProcessor(
            OrderComponentRepository componentRepository,
            ComponentAttemptRepository attemptRepository,
            ComponentStateMachine stateMachine,
            EventService eventService,
            UnknownResolver unknownResolver
    ) {
        this.componentRepository = componentRepository;
        this.attemptRepository = attemptRepository;
        this.stateMachine = stateMachine;
        this.eventService = eventService;
        this.unknownResolver = unknownResolver;
    }

    /**
     * Начинает резервирование компонента.
     *
     * Проверяет допустимость перехода в RESERVING,
     * создаёт запись о попытке и сохраняет новое состояние компонента.
     *
     * @param componentId идентификатор компонента
     * @return созданная попытка резервирования
     * @throws OrchestratorException если компонент не найден
     * или переход недопустим
     */
    @Transactional
    public ComponentAttempt startReserve(Long componentId)
            throws OrchestratorException {

        OrderComponent component = getComponent(componentId);

        ComponentStatus previousStatus = component.getStatus();
        ComponentStatus newStatus = ComponentStatus.RESERVING;

        stateMachine.assertAllowed(previousStatus, newStatus);

        ComponentAttempt attempt = createAttempt(component, AttemptOperation.RESERVE);

        component.changeStatus(newStatus);

        componentRepository.save(component);
        attemptRepository.save(attempt);

        eventService.record(
                component.getTripOrder().getId(),
                componentId,
                EventType.STATUS_CHANGED,
                previousStatus.name(),
                newStatus.name(),
                "Начато резервирование компонента",
                null
        );

        return attempt;
    }

    /**
     * Завершает попытку резервирования.
     *
     * SUCCESS переводит компонент в RESERVED.
     * FAILED переводит компонент в FAILED.
     * TIMEOUT переводит компонент в UNKNOWN.
     *
     * @param componentId идентификатор компонента
     * @param attempt попытка резервирования
     * @param result результат операции
     * @param externalBookingId идентификатор бронирования у поставщика
     * @param reason пояснение результата
     * @throws OrchestratorException если компонент не найден
     * или переход недопустим
     */
    @Transactional
    public void completeReserve(
            Long componentId,
            ComponentAttempt attempt,
            AttemptResult result,
            String externalBookingId,
            String reason
    ) throws OrchestratorException {

        OrderComponent component = getComponent(componentId);

        validateAttempt(attempt, componentId, AttemptOperation.RESERVE);
        validatePendingAttempt(attempt);

        if (component.getStatus() != ComponentStatus.RESERVING) {
            throw new InvalidRequestException(
                    "Компонент не находится в состоянии RESERVING"
            );
        }

        if (result == AttemptResult.SUCCESS
                && (externalBookingId == null || externalBookingId.isBlank())) {
            throw new InvalidRequestException(
                    "Для успешного резервирования необходим externalBookingId"
            );
        }

        ComponentStatus newStatus = switch (result) {
            case SUCCESS -> ComponentStatus.RESERVED;
            case FAILED -> ComponentStatus.FAILED;
            case TIMEOUT -> ComponentStatus.UNKNOWN;
        };

        ComponentStatus previousStatus = component.getStatus();

        stateMachine.assertAllowed(previousStatus, newStatus);

        if (result == AttemptResult.SUCCESS) {
            component.setExternalBookingId(externalBookingId);
        }

        component.changeStatus(newStatus);
        attempt.finish(result, reason);

        componentRepository.save(component);
        attemptRepository.save(attempt);

        EventType eventType = switch (result) {
            case SUCCESS -> EventType.COMPONENT_RESERVED;
            case FAILED -> EventType.COMPONENT_RESERVE_FAILED;
            case TIMEOUT -> EventType.STATUS_CHANGED;
        };

        eventService.record(
                component.getTripOrder().getId(),
                componentId,
                eventType,
                previousStatus.name(),
                newStatus.name(),
                reason,
                null
        );
    }

    /**
     * Начинает подтверждение зарезервированного компонента.
     *
     * Сам статус компонента остаётся RESERVED до получения
     * результата от поставщика.
     *
     * @param componentId идентификатор компонента
     * @return созданная попытка подтверждения
     * @throws OrchestratorException если компонент не найден
     * или находится в неподходящем состоянии
     */
    @Transactional
    public ComponentAttempt startConfirm(Long componentId)
            throws OrchestratorException {

        OrderComponent component = getComponent(componentId);

        if (component.getStatus() != ComponentStatus.RESERVED) {
            throw new InvalidRequestException(
                    "Подтверждать можно только зарезервированный компонент"
            );
        }

        ComponentAttempt attempt = createAttempt(component, AttemptOperation.CONFIRM);

        attemptRepository.save(attempt);
        eventService.record(
                component.getTripOrder().getId(),
                componentId,
                EventType.STATUS_CHANGED,
                ComponentStatus.RESERVED.name(),
                ComponentStatus.RESERVED.name(),
                "Начато подтверждение компонента",
                null
        );

        return attempt;
    }

    /**
     * Завершает попытку подтверждения компонента.
     *
     * SUCCESS переводит компонент в CONFIRMED.
     * FAILED оставляет компонент в RESERVED.
     * TIMEOUT переводит компонент в UNKNOWN.
     *
     * @param componentId идентификатор компонента
     * @param attempt попытка подтверждения
     * @param result результат операции
     * @param reason пояснение результата
     * @throws OrchestratorException если компонент не найден
     * или переход недопустим
     */
    @Transactional
    public void completeConfirm(
            Long componentId,
            ComponentAttempt attempt,
            AttemptResult result,
            String reason
    ) throws OrchestratorException {

        OrderComponent component = getComponent(componentId);

        validateAttempt(attempt, componentId, AttemptOperation.CONFIRM);
        validatePendingAttempt(attempt);

        if (component.getStatus() != ComponentStatus.RESERVED) {
            throw new InvalidRequestException(
                    "Компонент должен находиться в состоянии RESERVED"
            );
        }

        ComponentStatus newStatus = switch (result) {
            case SUCCESS -> ComponentStatus.CONFIRMED;
            case FAILED -> ComponentStatus.RESERVED;
            case TIMEOUT -> ComponentStatus.UNKNOWN;
        };

        ComponentStatus previousStatus = component.getStatus();

        if (newStatus != previousStatus) {
            stateMachine.assertAllowed(previousStatus, newStatus);
        }

        component.changeStatus(newStatus);
        attempt.finish(result, reason);

        componentRepository.save(component);
        attemptRepository.save(attempt);

        EventType eventType = switch (result) {
            case SUCCESS -> EventType.COMPONENT_CONFIRMED;
            case FAILED -> EventType.COMPONENT_CONFIRM_FAILED;
            case TIMEOUT -> EventType.STATUS_CHANGED;
        };

        eventService.record(
                component.getTripOrder().getId(),
                componentId,
                eventType,
                previousStatus.name(),
                newStatus.name(),
                reason,
                null
        );
    }

    /**
     * Начинает отмену бронирования компонента.
     *
     * Переводит компонент в COMPENSATING и создаёт попытку отмены.
     *
     * @param componentId идентификатор компонента
     * @return созданная попытка отмены
     * @throws OrchestratorException если компонент не найден
     * или переход недопустим
     */
    @Transactional
    public ComponentAttempt startCancel(Long componentId)
            throws OrchestratorException {

        OrderComponent component = getComponent(componentId);

        ComponentStatus previousStatus = component.getStatus();
        ComponentStatus newStatus = ComponentStatus.COMPENSATING;

        stateMachine.assertAllowed(previousStatus, newStatus);

        ComponentAttempt attempt = createAttempt(component, AttemptOperation.CANCEL);

        component.changeStatus(newStatus);

        componentRepository.save(component);
        attemptRepository.save(attempt);

        eventService.record(
                component.getTripOrder().getId(),
                componentId,
                EventType.COMPENSATION_STARTED,
                previousStatus.name(),
                newStatus.name(),
                "Начата отмена бронирования компонента",
                null
        );

        return attempt;
    }

    /**
     * Завершает попытку отмены бронирования.
     *
     * SUCCESS переводит компонент в CANCELLED.
     * FAILED оставляет компонент в COMPENSATING для последующей обработки.
     * TIMEOUT переводит компонент в UNKNOWN.
     *
     * @param componentId идентификатор компонента
     * @param attempt попытка отмены
     * @param result результат операции
     * @param reason пояснение результата
     * @throws OrchestratorException если компонент не найден
     * или переход недопустим
     */
    @Transactional
    public void completeCancel(
            Long componentId,
            ComponentAttempt attempt,
            AttemptResult result,
            String reason
    ) throws OrchestratorException {

        OrderComponent component = getComponent(componentId);

        validateAttempt(attempt, componentId, AttemptOperation.CANCEL);
        validatePendingAttempt(attempt);

        if (component.getStatus() != ComponentStatus.COMPENSATING) {
            throw new InvalidRequestException(
                    "Компонент не находится в состоянии COMPENSATING"
            );
        }

        ComponentStatus newStatus = switch (result) {
            case SUCCESS -> ComponentStatus.CANCELLED;
            case FAILED -> ComponentStatus.COMPENSATING;
            case TIMEOUT -> ComponentStatus.UNKNOWN;
        };

        ComponentStatus previousStatus = component.getStatus();

        if (newStatus != previousStatus) {
            stateMachine.assertAllowed(previousStatus, newStatus);
        }

        component.changeStatus(newStatus);
        attempt.finish(result, reason);

        componentRepository.save(component);
        attemptRepository.save(attempt);

        if (result == AttemptResult.SUCCESS) {
            eventService.record(
                    component.getTripOrder().getId(),
                    componentId,
                    EventType.COMPONENT_CANCELLED,
                    previousStatus.name(),
                    newStatus.name(),
                    reason,
                    null
            );
        } else if (result == AttemptResult.TIMEOUT) {
            eventService.record(
                    component.getTripOrder().getId(),
                    componentId,
                    EventType.STATUS_CHANGED,
                    previousStatus.name(),
                    newStatus.name(),
                    reason,
                    null
            );
        }
    }

    /**
     * Передаёт разрешение неопределённого состояния
     * специализированному сервису UnknownResolver.
     *
     * @param componentId идентификатор компонента
     * @param originalOperation исходная операция
     * @param resolution результат проверки статуса
     * @param externalBookingId идентификатор бронирования
     * @param reason пояснение результата
     * @throws OrchestratorException если разрешение невозможно
     */
    @Transactional
    public void resolveUnknown(
            Long componentId,
            AttemptOperation originalOperation,
            UnknownResolution resolution,
            String externalBookingId,
            String reason
    ) throws OrchestratorException {

        unknownResolver.resolve(
                componentId,
                originalOperation,
                resolution,
                externalBookingId,
                reason
        );
    }

    /**
     * Находит компонент по идентификатору.
     */
    private OrderComponent getComponent(Long componentId)
            throws ComponentNotFoundException {

        return componentRepository.findById(componentId)
                .orElseThrow(() -> new ComponentNotFoundException(componentId));
    }

    /**
     * Создаёт новую попытку выполнения операции.
     *
     * Результат остаётся null до завершения операции.
     */
    private ComponentAttempt createAttempt(
            OrderComponent component,
            AttemptOperation operation
    ) {
        String requestId = component.getTripOrder().getId()
                + ":" + component.getId()
                + ":" + operation.name();

        return new ComponentAttempt(
                component.getId(),
                operation,
                requestId
        );
    }

    /**
     * Проверяет, что попытка соответствует ожидаемой операции.
     */
    private void validateAttempt(
            ComponentAttempt attempt,
            Long componentId,
            AttemptOperation expectedOperation
    ) throws InvalidRequestException {

        if (attempt == null
                || !componentId.equals(attempt.getComponentId())
                || attempt.getOperation() != expectedOperation) {
            throw new InvalidRequestException(
                    "Попытка не соответствует компоненту или операции "
                            + expectedOperation
            );
        }
    }

    /**
     * Проверяет, что попытка ещё не была завершена.
     */
    private void validatePendingAttempt(ComponentAttempt attempt)
            throws InvalidRequestException {

        if (attempt.getResult() != null) {
            throw new InvalidRequestException(
                    "Эта попытка уже была завершена"
            );
        }
    }
}
