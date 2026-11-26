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
import ru.ttravel.orchestrator.domain.repositories.OrderRepository;
import ru.ttravel.orchestrator.order.OrderTransitionService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompensationServiceTest {

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final ComponentAttemptRepository attemptRepository =
            mock(ComponentAttemptRepository.class);
    private final ComponentProcessor processor = mock(ComponentProcessor.class);
    private final OrderTransitionService transitionService =
            mock(OrderTransitionService.class);

    private final CompensationService service = new CompensationService(
            orderRepository,
            attemptRepository,
            processor,
            transitionService
    );

    private TripOrder order;
    private OrderComponent flight;
    private OrderComponent hotel;

    @BeforeEach
    void setUp() {
        order = new TripOrder(10L, "RUB", "key", "hash");
        ReflectionTestUtils.setField(order, "id", 1L);

        flight = new OrderComponent(
                ComponentType.FLIGHT, 1000, "RUB", "flight", "{}"
        );
        ReflectionTestUtils.setField(flight, "id", 11L);
        flight.changeStatus(ComponentStatus.RESERVED);

        hotel = new OrderComponent(
                ComponentType.HOTEL, 2000, "RUB", "hotel", "{}"
        );
        ReflectionTestUtils.setField(hotel, "id", 12L);
        hotel.changeStatus(ComponentStatus.CONFIRMED);

        order.addComponent(flight);
        order.addComponent(hotel);

        when(orderRepository.findWithComponentsById(1L))
                .thenReturn(Optional.of(order));

        when(attemptRepository
                .existsByComponentIdAndOperationAndResultIsNull(
                        anyLong(), eq(AttemptOperation.CANCEL)
                )).thenReturn(false);
    }

    @Test
    void startCompensationFromActionRequiredStartsWithLastComponent()
            throws Exception {
        order.changeStatus(OrderStatus.ACTION_REQUIRED);

        ComponentAttempt attempt = new ComponentAttempt(
                12L, AttemptOperation.CANCEL, "request"
        );
        when(processor.startCancel(12L)).thenReturn(attempt);

        ComponentAttempt result = service.startCompensation(1L, 10L);

        assertSame(attempt, result);
        verify(transitionService).transition(
                order,
                OrderStatus.CANCELLING,
                EventType.COMPENSATION_STARTED,
                "Начата отмена заказа"
        );
        verify(processor).startCancel(12L);
        verify(processor, never()).startCancel(11L);
    }

    @Test
    void startCompensationRejectsConfirmingOrder() {
        order.changeStatus(OrderStatus.CONFIRMING);

        assertThrows(
                InvalidRequestException.class,
                () -> service.startCompensation(1L, 10L)
        );

        verifyNoInteractions(processor);
        verifyNoInteractions(transitionService);
    }

    @Test
    void startCompensationSkipsAlreadyCancelledComponentsAndFinishesOrder()
            throws Exception {
        flight.changeStatus(ComponentStatus.CANCELLED);
        hotel.changeStatus(ComponentStatus.CANCELLED);
        order.changeStatus(OrderStatus.CANCELLING);

        ComponentAttempt result = service.startCompensation(1L, 10L);

        assertNull(result);
        verify(transitionService).transition(
                order,
                OrderStatus.CANCELLED,
                EventType.ORDER_CANCELLED,
                "Компенсация всех компонентов завершена"
        );
        verifyNoInteractions(processor);
    }

    @Test
    void startCompensationWaitsWhenComponentIsUnknown() throws Exception {
        hotel.changeStatus(ComponentStatus.UNKNOWN);
        order.changeStatus(OrderStatus.CANCELLING);

        ComponentAttempt result = service.startCompensation(1L, 10L);

        assertNull(result);
        verifyNoInteractions(processor);
        verify(transitionService, never()).transition(
                any(), eq(OrderStatus.CANCELLED), any(), anyString()
        );
    }

    @Test
    void failedCancellationMovesCancellingOrderToNeedsHelp() throws Exception {
        order.changeStatus(OrderStatus.CANCELLING);

        ComponentAttempt attempt = new ComponentAttempt(
                12L, AttemptOperation.CANCEL, "request"
        );

        service.handleCancellationResult(
                1L, 10L, 12L, attempt, AttemptResult.FAILED, "provider error"
        );

        verify(processor).completeCancel(
                12L, attempt, AttemptResult.FAILED, "provider error"
        );
        verify(transitionService).transition(
                order,
                OrderStatus.NEEDS_HELP,
                EventType.COMPENSATION_FAILED,
                "Не удалось отменить компонент 12"
        );
    }

    @Test
    void timeoutDuringCancellationDoesNotStartNextComponent() throws Exception {
        order.changeStatus(OrderStatus.CANCELLING);

        ComponentAttempt attempt = new ComponentAttempt(
                12L, AttemptOperation.CANCEL, "request"
        );

        ComponentAttempt result = service.handleCancellationResult(
                1L, 10L, 12L, attempt, AttemptResult.TIMEOUT, "timeout"
        );

        assertNull(result);
        verify(processor).completeCancel(
                12L, attempt, AttemptResult.TIMEOUT, "timeout"
        );
        verify(processor, never()).startCancel(anyLong());
    }
}
