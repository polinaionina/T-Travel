package ru.ttravel.orchestrator.domain.state;

import org.junit.jupiter.api.Test;
import ru.ttravel.orchestrator.domain.exceptions.InvalidStatusTransitionException;
import ru.ttravel.orchestrator.domain.models.OrderStatus;
import static org.junit.jupiter.api.Assertions.*;

class OrderStateMachineTest {
    private final OrderStateMachine machine = new OrderStateMachine();

    @Test 
    void allowsBookingAndCompensationFlow() {
        assertTrue(machine.isAllowed(OrderStatus.DRAFT, OrderStatus.CONFIRMING));
        assertTrue(machine.isAllowed(OrderStatus.CONFIRMING, OrderStatus.ACTION_REQUIRED));
        assertTrue(machine.isAllowed(OrderStatus.ACTION_REQUIRED, OrderStatus.CANCELLING));
        assertTrue(machine.isAllowed(OrderStatus.CANCELLING, OrderStatus.CANCELLED));
    }

    @Test 
    void doesNotAllowDirectCancellationFromConfirming() {
        assertFalse(machine.isAllowed(OrderStatus.CONFIRMING, OrderStatus.CANCELLING));
        assertThrows(InvalidStatusTransitionException.class,
                () -> machine.assertAllowed(OrderStatus.CONFIRMING, OrderStatus.CANCELLING));
    }
}
