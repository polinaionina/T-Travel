package ru.ttravel.orchestrator.booking;

import org.junit.jupiter.api.Test;

import ru.ttravel.orchestrator.domain.data.ComponentAttempt;
import ru.ttravel.orchestrator.domain.data.OrderComponent;
import ru.ttravel.orchestrator.domain.data.TripOrder;
import ru.ttravel.orchestrator.domain.exceptions.InvalidRequestException;
import ru.ttravel.orchestrator.domain.exceptions.OrchestratorException;
import ru.ttravel.orchestrator.domain.exceptions.OrderNotFoundException;
import ru.ttravel.orchestrator.domain.models.*;
import ru.ttravel.orchestrator.domain.repositories.ComponentAttemptRepository;
import ru.ttravel.orchestrator.order.OrderService;
import ru.ttravel.orchestrator.order.OrderTransitionService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.springframework.test.util.ReflectionTestUtils;

class BookingServiceTest {
    private final OrderService orderService = mock(OrderService.class);
    private final OrderTransitionService transitionService = mock(OrderTransitionService.class);
    private final ComponentProcessor processor = mock(ComponentProcessor.class);
    private final ComponentAttemptRepository attempts = mock(ComponentAttemptRepository.class);
    private final BookingService service = new BookingService(orderService, transitionService, processor, attempts);

    private TripOrder readyOrder() {
        TripOrder order = new TripOrder(1L, "RUB", "key", "hash");
        OrderComponent flight = new OrderComponent(ComponentType.FLIGHT, 1000, "RUB", "f", "{}");
        OrderComponent hotel = new OrderComponent(ComponentType.HOTEL, 2000, "RUB", "h", "{}");
        flight.changeStatus(ComponentStatus.RESERVED);
        hotel.changeStatus(ComponentStatus.RESERVED);
        order.addComponent(flight);
        order.addComponent(hotel);
        return order;
    }

    @Test 
    void startBookingTransitionsAndStartsFirstComponent() throws OrchestratorException {
        TripOrder order = readyOrder();
        when(orderService.getOrder(10L, 1L)).thenReturn(order);
        when(attempts.existsByComponentIdAndOperationAndResultIsNull(11L, AttemptOperation.CONFIRM)).thenReturn(false);
        ComponentAttempt attempt = new ComponentAttempt(null, AttemptOperation.CONFIRM, "req");
        when(processor.startConfirm(null)).thenReturn(attempt);

        assertSame(attempt, service.startBooking(10L, 1L));
        verify(transitionService).transition(order, OrderStatus.CONFIRMING, EventType.STATUS_CHANGED,
                "Начато подтверждение компонентов заказа");
    }

    @Test 
    void startBookingRejectsMissingRequiredComponent() throws OrderNotFoundException {
        TripOrder order = new TripOrder(1L, "RUB", "key", "hash");
        OrderComponent flight = new OrderComponent(ComponentType.FLIGHT, 1000, "RUB", "f", "{}");
        flight.changeStatus(ComponentStatus.RESERVED);
        order.addComponent(flight);
        when(orderService.getOrder(10L, 1L)).thenReturn(order);
        assertThrows(InvalidRequestException.class, () -> service.startBooking(10L, 1L));
        verifyNoInteractions(processor);
    }

    @Test
    void timeoutDoesNotTransitionOrderOutOfConfirming() throws OrchestratorException {
        TripOrder order = readyOrder();
        order.changeStatus(OrderStatus.CONFIRMING);

        OrderComponent flight = order.activeComponents().stream()
                .filter(component -> component.getType() == ComponentType.FLIGHT)
                .findFirst()
                .orElseThrow();

        ReflectionTestUtils.setField(flight, "id", 11L);

        when(orderService.getOrder(10L, 1L)).thenReturn(order);

        ComponentAttempt attempt = new ComponentAttempt(
                11L,
                AttemptOperation.CONFIRM,
                "req"
        );

        service.handleConfirmationResult(
                10L,
                1L,
                11L,
                attempt,
                AttemptResult.TIMEOUT,
                "timeout"
        );

        verify(processor).completeConfirm(
                11L,
                attempt,
                AttemptResult.TIMEOUT,
                "timeout"
        );

        verifyNoInteractions(transitionService);
    }
}
