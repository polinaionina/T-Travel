package ru.ttravel.orchestrator.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import ru.ttravel.orchestrator.domain.data.ComponentAttempt;
import ru.ttravel.orchestrator.domain.data.OrderComponent;
import ru.ttravel.orchestrator.domain.data.TripOrder;
import ru.ttravel.orchestrator.domain.exceptions.InvalidRequestException;
import ru.ttravel.orchestrator.domain.models.*;
import ru.ttravel.orchestrator.domain.repositories.ComponentAttemptRepository;
import ru.ttravel.orchestrator.domain.repositories.OrderComponentRepository;
import ru.ttravel.orchestrator.domain.state.ComponentStateMachine;
import ru.ttravel.orchestrator.events.EventService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ComponentProcessorTest {

    private final OrderComponentRepository componentRepository =
            mock(OrderComponentRepository.class);
    private final ComponentAttemptRepository attemptRepository =
            mock(ComponentAttemptRepository.class);
    private final ComponentStateMachine stateMachine =
            new ComponentStateMachine();
    private final EventService eventService = mock(EventService.class);
    private final UnknownResolver unknownResolver = mock(UnknownResolver.class);

    private final ComponentProcessor processor = new ComponentProcessor(
            componentRepository,
            attemptRepository,
            stateMachine,
            eventService,
            unknownResolver
    );

    private TripOrder order;
    private OrderComponent component;

    @BeforeEach
    void setUp() {
        order = new TripOrder(10L, "RUB", "key", "hash");
        ReflectionTestUtils.setField(order, "id", 1L);

        component = new OrderComponent(
                ComponentType.FLIGHT, 1000, "RUB", "flight-offer", "{}"
        );
        ReflectionTestUtils.setField(component, "id", 11L);

        order.addComponent(component);

        when(componentRepository.findById(11L))
                .thenReturn(Optional.of(component));
    }

    @Test
    void startReserveChangesStatusAndCreatesAttempt() throws Exception {
        ComponentAttempt attempt = processor.startReserve(11L);

        assertEquals(ComponentStatus.RESERVING, component.getStatus());
        assertEquals(11L, attempt.getComponentId());
        assertEquals(AttemptOperation.RESERVE, attempt.getOperation());
        assertNull(attempt.getResult());

        verify(componentRepository).save(component);
        verify(attemptRepository).save(attempt);
        verify(eventService).record(
                eq(1L), eq(11L), eq(EventType.STATUS_CHANGED),
                eq("PENDING"), eq("RESERVING"),
                anyString(), isNull()
        );
    }

    @Test
    void completeReserveSuccessChangesStatusAndStoresExternalId() throws Exception {
        component.changeStatus(ComponentStatus.RESERVING);
        ComponentAttempt attempt =
                new ComponentAttempt(11L, AttemptOperation.RESERVE, "request-1");

        processor.completeReserve(
                11L, attempt, AttemptResult.SUCCESS, "external-123", "ok"
        );

        assertEquals(ComponentStatus.RESERVED, component.getStatus());
        assertEquals("external-123", component.getExternalBookingId());
        assertEquals(AttemptResult.SUCCESS, attempt.getResult());

        verify(componentRepository).save(component);
        verify(attemptRepository).save(attempt);
        verify(eventService).record(
                eq(1L), eq(11L), eq(EventType.COMPONENT_RESERVED),
                eq("RESERVING"), eq("RESERVED"),
                eq("ok"), isNull()
        );
    }

    @Test
    void completeReserveRejectsSuccessWithoutExternalBookingId() {
        component.changeStatus(ComponentStatus.RESERVING);
        ComponentAttempt attempt =
                new ComponentAttempt(11L, AttemptOperation.RESERVE, "request-1");

        assertThrows(
                InvalidRequestException.class,
                () -> processor.completeReserve(
                        11L, attempt, AttemptResult.SUCCESS, null, "ok"
                )
        );

        assertEquals(ComponentStatus.RESERVING, component.getStatus());
        assertNull(attempt.getResult());
        verify(componentRepository, never()).save(any());
        verify(attemptRepository, never()).save(any());
    }

    @Test
    void startConfirmCreatesAttemptForReservedComponent() throws Exception {
        component.changeStatus(ComponentStatus.RESERVED);

        ComponentAttempt attempt = processor.startConfirm(11L);

        assertEquals(AttemptOperation.CONFIRM, attempt.getOperation());
        assertEquals(11L, attempt.getComponentId());
        assertNull(attempt.getResult());

        verify(attemptRepository).save(attempt);
        verify(eventService).record(
                eq(1L), eq(11L), eq(EventType.STATUS_CHANGED),
                eq("RESERVED"), eq("RESERVED"),
                anyString(), isNull()
        );
    }

    @Test
    void startConfirmRejectsComponentThatIsNotReserved() {
        assertThrows(
                InvalidRequestException.class,
                () -> processor.startConfirm(11L)
        );

        verifyNoInteractions(attemptRepository);
    }

    @Test
    void completeConfirmTimeoutChangesComponentToUnknown() throws Exception {
        component.changeStatus(ComponentStatus.RESERVED);
        ComponentAttempt attempt =
                new ComponentAttempt(11L, AttemptOperation.CONFIRM, "request-2");

        processor.completeConfirm(
                11L, attempt, AttemptResult.TIMEOUT, "timeout"
        );

        assertEquals(ComponentStatus.UNKNOWN, component.getStatus());
        assertEquals(AttemptResult.TIMEOUT, attempt.getResult());

        verify(componentRepository).save(component);
        verify(attemptRepository).save(attempt);
        verify(eventService).record(
                eq(1L), eq(11L), eq(EventType.STATUS_CHANGED),
                eq("RESERVED"), eq("UNKNOWN"),
                eq("timeout"), isNull()
        );
    }

    @Test
    void completeConfirmRejectsAlreadyFinishedAttempt() {
        component.changeStatus(ComponentStatus.RESERVED);
        ComponentAttempt attempt =
                new ComponentAttempt(11L, AttemptOperation.CONFIRM, "request-2");
        attempt.finish(AttemptResult.FAILED, "previous failure");

        assertThrows(
                InvalidRequestException.class,
                () -> processor.completeConfirm(
                        11L, attempt, AttemptResult.SUCCESS, "ok"
                )
        );

        verify(componentRepository, never()).save(any());
    }

    @Test
    void startCancelMovesReservedComponentToCompensating() throws Exception {
        component.changeStatus(ComponentStatus.RESERVED);
        when(attemptRepository
                .existsByComponentIdAndOperationAndResultIsNull(
                        11L, AttemptOperation.CANCEL
                )).thenReturn(false);

        ComponentAttempt attempt = processor.startCancel(11L);

        assertEquals(ComponentStatus.COMPENSATING, component.getStatus());
        assertEquals(AttemptOperation.CANCEL, attempt.getOperation());
        assertNull(attempt.getResult());

        verify(componentRepository).save(component);
        verify(attemptRepository).save(attempt);
    }

    @Test
    void startCancelRejectsWhenCancellationIsAlreadyPending() {
        component.changeStatus(ComponentStatus.RESERVED);
        when(attemptRepository
                .existsByComponentIdAndOperationAndResultIsNull(
                        11L, AttemptOperation.CANCEL
                )).thenReturn(true);

        assertThrows(
                InvalidRequestException.class,
                () -> processor.startCancel(11L)
        );

        assertEquals(ComponentStatus.RESERVED, component.getStatus());
        verify(attemptRepository, never()).save(any());
    }

    @Test
    void completeCancelSuccessChangesStatusToCancelled() throws Exception {
        component.changeStatus(ComponentStatus.COMPENSATING);
        ComponentAttempt attempt =
                new ComponentAttempt(11L, AttemptOperation.CANCEL, "request-3");

        processor.completeCancel(
                11L, attempt, AttemptResult.SUCCESS, "cancelled"
        );

        assertEquals(ComponentStatus.CANCELLED, component.getStatus());
        assertEquals(AttemptResult.SUCCESS, attempt.getResult());

        verify(componentRepository).save(component);
        verify(attemptRepository).save(attempt);
        verify(eventService).record(
                eq(1L), eq(11L), eq(EventType.COMPONENT_CANCELLED),
                eq("COMPENSATING"), eq("CANCELLED"),
                eq("cancelled"), isNull()
        );
    }
}
