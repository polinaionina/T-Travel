package ru.ttravel.orchestrator.domain.state;

import org.junit.jupiter.api.Test;

import ru.ttravel.orchestrator.domain.exceptions.InvalidStatusTransitionException;
import ru.ttravel.orchestrator.domain.models.ComponentStatus;
import ru.ttravel.orchestrator.domain.models.OrderStatus;

import static org.junit.jupiter.api.Assertions.*;

class StateMachineTest {

    private final OrderStateMachine orderStateMachine =
            new OrderStateMachine();

    private final ComponentStateMachine componentStateMachine =
            new ComponentStateMachine();

    @Test
    void draftCanTransitionToConfirming() {
        assertTrue(orderStateMachine.isAllowed(
                OrderStatus.DRAFT, OrderStatus.CONFIRMING
        ));
    }

    @Test
    void confirmingCannotTransitionDirectlyToCancelling() {
        assertFalse(orderStateMachine.isAllowed(
                OrderStatus.CONFIRMING, OrderStatus.CANCELLING
        ));
    }

    @Test
    void actionRequiredCanTransitionToCancelling() {
        assertTrue(orderStateMachine.isAllowed(
                OrderStatus.ACTION_REQUIRED, OrderStatus.CANCELLING
        ));
    }

    @Test
    void confirmedOrderCannotChangeStatus() {
        assertFalse(orderStateMachine.isAllowed(
                OrderStatus.CONFIRMED, OrderStatus.CANCELLED
        ));
        assertTrue(orderStateMachine.allowedFrom(OrderStatus.CONFIRMED).isEmpty());
    }

    @Test
    void assertAllowedThrowsForForbiddenOrderTransition() {
        assertThrows(
                InvalidStatusTransitionException.class,
                () -> orderStateMachine.assertAllowed(
                        OrderStatus.CONFIRMED, OrderStatus.CANCELLED
                )
        );
    }

    @Test
    void pendingComponentCanStartReservation() {
        assertTrue(componentStateMachine.isAllowed(
                ComponentStatus.PENDING, ComponentStatus.RESERVING
        ));
    }

    @Test
    void reservedComponentCanBeConfirmed() {
        assertTrue(componentStateMachine.isAllowed(
                ComponentStatus.RESERVED, ComponentStatus.CONFIRMED
        ));
    }

    @Test
    void unknownComponentCanBeResolvedAsConfirmed() {
        assertTrue(componentStateMachine.isAllowed(
                ComponentStatus.UNKNOWN, ComponentStatus.CONFIRMED
        ));
    }

    @Test
    void failedComponentCannotBeReservedAgain() {
        assertFalse(componentStateMachine.isAllowed(
                ComponentStatus.FAILED, ComponentStatus.RESERVING
        ));
    }

    @Test
    void cancelledComponentIsTerminal() {
        assertTrue(componentStateMachine.allowedFrom(
                ComponentStatus.CANCELLED
        ).isEmpty());
    }
}
