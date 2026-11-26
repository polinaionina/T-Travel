package ru.ttravel.orchestrator.domain.models;

import org.junit.jupiter.api.Test;
import ru.ttravel.orchestrator.domain.data.OrderComponent;
import ru.ttravel.orchestrator.domain.data.TripOrder;
import static org.junit.jupiter.api.Assertions.*;

class TripOrderTest {
    @Test 
    void newOrderStartsAsDraftWithZeroPrice() {
        TripOrder order = new TripOrder(7L, "RUB", "key", "hash");
        assertEquals(7L, order.getUserId());
        assertEquals(OrderStatus.DRAFT, order.getStatus());
        assertEquals(0L, order.getTotalPrice());
        assertEquals("RUB", order.getCurrency());
    }

    @Test 
    void addComponentUpdatesTotalAndActiveComponents() {
        TripOrder order = new TripOrder(7L, "RUB", "key", "hash");
        OrderComponent flight = new OrderComponent(ComponentType.FLIGHT, 10000, "RUB", "f1", "{}");
        OrderComponent hotel = new OrderComponent(ComponentType.HOTEL, 20000, "RUB", "h1", "{}");
        order.addComponent(flight);
        order.addComponent(hotel);
        assertEquals(30000L, order.getTotalPrice());
        assertEquals(2, order.activeComponents().size());
        assertSame(order, flight.getTripOrder());
    }

    @Test 
    void activeComponentsUsesLatestComponentOfEachType() {
        TripOrder order = new TripOrder(7L, "RUB", "key", "hash");
        OrderComponent oldFlight = new OrderComponent(ComponentType.FLIGHT, 10000, "RUB", "f1", "{}");
        OrderComponent newFlight = new OrderComponent(ComponentType.FLIGHT, 15000, "RUB", "f2", "{}");
        order.addComponent(oldFlight);
        order.addComponent(newFlight);
        assertEquals(1, order.activeComponents().size());
        assertSame(newFlight, order.activeComponents().get(0));
    }

    @Test 
    void recalculateTotalCountsOnlyReservedAndConfirmedActiveComponents() {
        TripOrder order = new TripOrder(7L, "RUB", "key", "hash");
        OrderComponent flight = new OrderComponent(ComponentType.FLIGHT, 10000, "RUB", "f", "{}");
        OrderComponent hotel = new OrderComponent(ComponentType.HOTEL, 20000, "RUB", "h", "{}");
        order.addComponent(flight);
        order.addComponent(hotel);
        flight.changeStatus(ComponentStatus.RESERVED);
        hotel.changeStatus(ComponentStatus.PENDING);
        order.recalculateTotal();
        assertEquals(10000L, order.getTotalPrice());
    }
}
