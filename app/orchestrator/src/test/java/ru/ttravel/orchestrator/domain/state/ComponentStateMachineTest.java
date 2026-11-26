package ru.ttravel.orchestrator.domain.state;

import org.junit.jupiter.api.Test;
import ru.ttravel.orchestrator.domain.exceptions.InvalidStatusTransitionException;
import ru.ttravel.orchestrator.domain.models.ComponentStatus;
import static org.junit.jupiter.api.Assertions.*;

class ComponentStateMachineTest {
    private final ComponentStateMachine machine = new ComponentStateMachine();

    @Test 
    void allowsExpectedReservationAndConfirmationTransitions() {
        assertTrue(machine.isAllowed(ComponentStatus.PENDING, ComponentStatus.RESERVING));
        assertTrue(machine.isAllowed(ComponentStatus.RESERVING, ComponentStatus.RESERVED));
        assertTrue(machine.isAllowed(ComponentStatus.RESERVED, ComponentStatus.CONFIRMED));
        assertTrue(machine.isAllowed(ComponentStatus.UNKNOWN, ComponentStatus.CONFIRMED));
    }

    @Test 
    void rejectsConfirmationFromPending() {
        assertFalse(machine.isAllowed(ComponentStatus.PENDING, ComponentStatus.CONFIRMED));
        assertThrows(InvalidStatusTransitionException.class,
                () -> machine.assertAllowed(ComponentStatus.PENDING, ComponentStatus.CONFIRMED));
    }
}
