package ru.ttravel.orchestrator.domain.models;

import org.junit.jupiter.api.Test;
import ru.ttravel.orchestrator.domain.data.OrderComponent;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class OrderComponentTest {
    @Test 
    void newComponentStartsPendingAndUsesTypeStep() {
        OrderComponent c = new OrderComponent(ComponentType.FLIGHT, 5000, "RUB", "offer", "{}");
        assertEquals(ComponentStatus.PENDING, c.getStatus());
        assertEquals(ComponentType.FLIGHT, c.getType());
        assertEquals(ComponentType.FLIGHT.getStepNo(), c.getStepNo());
        assertEquals(5000L, c.getPrice());
    }

    @Test 
    void settersUpdateMutableFields() {
        OrderComponent c = new OrderComponent(ComponentType.HOTEL, 5000, "RUB", "offer", "{}");
        Instant deadline = Instant.now();
        c.setPrice(7000);
        c.setDeadlineAt(deadline);
        c.setExternalBookingId("booking-1");
        assertEquals(7000L, c.getPrice());
        assertEquals(deadline, c.getDeadlineAt());
        assertEquals("booking-1", c.getExternalBookingId());
    }
}
