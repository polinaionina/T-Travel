package ru.ttravel.orchestrator.booking;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.ttravel.orchestrator.domain.data.ComponentAttempt;
import ru.ttravel.orchestrator.domain.data.OrderComponent;
import ru.ttravel.orchestrator.domain.exceptions.ComponentNotFoundException;
import ru.ttravel.orchestrator.domain.exceptions.InvalidRequestException;
import ru.ttravel.orchestrator.domain.exceptions.OrchestratorException;
import ru.ttravel.orchestrator.domain.models.AttemptOperation;
import ru.ttravel.orchestrator.domain.models.AttemptResult;
import ru.ttravel.orchestrator.domain.models.ComponentStatus;
import ru.ttravel.orchestrator.domain.models.EventType;
import ru.ttravel.orchestrator.booking.models.UnknownResolution;
import ru.ttravel.orchestrator.domain.repositories.ComponentAttemptRepository;
import ru.ttravel.orchestrator.domain.repositories.OrderComponentRepository;
import ru.ttravel.orchestrator.domain.state.ComponentStateMachine;
import ru.ttravel.orchestrator.events.EventService;

import java.util.UUID;

@Service
public class UnknownResolver {

    private final OrderComponentRepository componentRepository;
    private final ComponentAttemptRepository attemptRepository;
    private final ComponentStateMachine stateMachine;
    private final EventService eventService;

    public UnknownResolver(
            OrderComponentRepository componentRepository,
            ComponentAttemptRepository attemptRepository,
            ComponentStateMachine stateMachine,
            EventService eventService
    ) {
        this.componentRepository = componentRepository;
        this.attemptRepository = attemptRepository;
        this.stateMachine = stateMachine;
        this.eventService = eventService;
    }

    /**
     * Разрешает неопределённый результат резервирования или подтверждения.
     *
     * Метод вызывается после получения результата проверки у поставщика.
     *
     * @param componentId идентификатор компонента
     * @param originalOperation операция, результат которой стал неизвестен
     * @param resolution результат проверки у поставщика
     * @param externalBookingId идентификатор бронирования у поставщика
     * @param reason пояснение результата проверки
     * @throws OrchestratorException если компонент не найден или переход недопустим
     */
    @Transactional
    public void resolve(
            Long componentId,
            AttemptOperation originalOperation,
            UnknownResolution resolution,
            String externalBookingId,
            String reason
    ) throws OrchestratorException {

        OrderComponent component = componentRepository.findById(componentId)
                .orElseThrow(() -> new ComponentNotFoundException(componentId));

        if (component.getStatus() != ComponentStatus.UNKNOWN) {
            throw new InvalidRequestException(
                    "Компонент " + componentId
                            + " не находится в состоянии UNKNOWN"
            );
        }

        if (originalOperation != AttemptOperation.RESERVE
                && originalOperation != AttemptOperation.CONFIRM) {
            throw new InvalidRequestException(
                    "UnknownResolver пока обрабатывает только RESERVE и CONFIRM"
            );
        }

        ComponentStatus newStatus = mapResolution(resolution);

        if (newStatus == ComponentStatus.UNKNOWN) {
            saveAttempt(componentId, AttemptResult.TIMEOUT, reason);
            return;
        }

        stateMachine.assertAllowed(component.getStatus(), newStatus);

        ComponentStatus previousStatus = component.getStatus();

        if (externalBookingId != null && !externalBookingId.isBlank()) {
            component.setExternalBookingId(externalBookingId);
        }

        component.changeStatus(newStatus);

        componentRepository.save(component);

        saveAttempt(componentId, AttemptResult.SUCCESS, reason);

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

    /**
     * Преобразует результат проверки поставщика
     * во внутренний статус компонента.
     */
    private ComponentStatus mapResolution(UnknownResolution resolution) {
        return switch (resolution) {
            case RESERVED -> ComponentStatus.RESERVED;
            case CONFIRMED -> ComponentStatus.CONFIRMED;
            case NOT_FOUND -> ComponentStatus.FAILED;
            case STILL_UNKNOWN -> ComponentStatus.UNKNOWN;
        };
    }

    /**
     * Сохраняет информацию о попытке проверки статуса.
     */
    private void saveAttempt(
            Long componentId,
            AttemptResult result,
            String reason
    ) {
        ComponentAttempt attempt = new ComponentAttempt(
                componentId,
                AttemptOperation.STATUS,
                UUID.randomUUID().toString()
        );

        attempt.finish(result, reason);

        attemptRepository.save(attempt);
    }
}
