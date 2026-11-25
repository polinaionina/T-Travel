package ru.ttravel.orchestrator.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import ru.ttravel.orchestrator.booking.models.UnknownResolution;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UnknownResolverTest {

    private final OrderComponentRepository componentRepository =
            mock(OrderComponentRepository.class);
    private final ComponentAttemptRepository attemptRepository =
            mock(ComponentAttemptRepository.class);
    private final ComponentStateMachine stateMachine =
            new ComponentStateMachine();
    private final EventService eventService = mock(EventService.class);

    private final UnknownResolver resolver = new UnknownResolver(
            componentRepository,
            attemptRepository,
            stateMachine,
            eventService
    );

    private OrderComponent component;

    @BeforeEach
    void setUp() {
        TripOrder order = new TripOrder(10L, "RUB", "key", "hash");
        ReflectionTestUtils.setField(order, "id", 1L);

        component = new OrderComponent(
                ComponentType.FLIGHT, 1000, "RUB", "offer", "{}"
        );
        ReflectionTestUtils.setField(component, "id", 11L);
        order.addComponent(component);
        component.changeStatus(ComponentStatus.UNKNOWN);

        when(componentRepository.findById(11L))
                .thenReturn(Optional.of(component));
    }

    @Test
    void confirmResolvedAsConfirmedChangesComponentStatus() throws Exception {
        resolver.resolve(
                11L,
                AttemptOperation.CONFIRM,
                UnknownResolution.CONFIRMED,
                "external-1",
                "provider says confirmed"
        );

        assertEquals(ComponentStatus.CONFIRMED, component.getStatus());
        assertEquals("external-1", component.getExternalBookingId());

        verify(componentRepository).save(component);
        verify(attemptRepository).save(argThat(attempt ->
                attempt.getOperation() == AttemptOperation.STATUS
                        && attempt.getResult() == AttemptResult.SUCCESS
        ));
        verify(eventService).record(
                eq(1L), eq(11L), eq(EventType.STATUS_CHANGED),
                eq("UNKNOWN"), eq("CONFIRMED"),
                eq("provider says confirmed"), isNull()
        );
    }

    @Test
    void confirmResolvedAsReservedRestoresReservedStatus() throws Exception {
        resolver.resolve(
                11L,
                AttemptOperation.CONFIRM,
                UnknownResolution.RESERVED,
                null,
                "provider says reserved"
        );

        assertEquals(ComponentStatus.RESERVED, component.getStatus());
        verify(componentRepository).save(component);
    }

    @Test
    void stillUnknownKeepsUnknownStatusAndSavesTimeoutAttempt() throws Exception {
        resolver.resolve(
                11L,
                AttemptOperation.CONFIRM,
                UnknownResolution.STILL_UNKNOWN,
                null,
                "still unknown"
        );

        assertEquals(ComponentStatus.UNKNOWN, component.getStatus());

        verify(attemptRepository).save(argThat(attempt ->
                attempt.getOperation() == AttemptOperation.STATUS
                        && attempt.getResult() == AttemptResult.TIMEOUT
        ));
        verify(componentRepository, never()).save(any());
        verifyNoInteractions(eventService);
    }

    @Test
    void cancelledIsRejectedForConfirmOperation() {
        assertThrows(
                InvalidRequestException.class,
                () -> resolver.resolve(
                        11L,
                        AttemptOperation.CONFIRM,
                        UnknownResolution.CANCELLED,
                        null,
                        "unexpected"
                )
        );

        assertEquals(ComponentStatus.UNKNOWN, component.getStatus());
        verify(attemptRepository, never()).save(any());
        verify(componentRepository, never()).save(any());
    }

    @Test
    void resolverRejectsComponentThatIsNotUnknown() {
        component.changeStatus(ComponentStatus.RESERVED);

        assertThrows(
                InvalidRequestException.class,
                () -> resolver.resolve(
                        11L,
                        AttemptOperation.CONFIRM,
                        UnknownResolution.CONFIRMED,
                        null,
                        "wrong state"
                )
        );

        verifyNoInteractions(attemptRepository);
    }

    @Test
    void cancelResolvedAsCancelledChangesStatusToCancelled() throws Exception {
        resolver.resolve(
                11L,
                AttemptOperation.CANCEL,
                UnknownResolution.CANCELLED,
                null,
                "already cancelled"
        );

        assertEquals(ComponentStatus.CANCELLED, component.getStatus());
        verify(componentRepository).save(component);
    }
}
