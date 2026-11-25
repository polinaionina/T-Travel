
package ru.ttravel.orchestrator.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.ttravel.orchestrator.domain.data.OrderEvent;
import ru.ttravel.orchestrator.domain.models.EventType;
import ru.ttravel.orchestrator.domain.repositories.OrderEventRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private OrderEventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    private OrderEvent event;

    @BeforeEach
    void setUp() {
        event = new OrderEvent(
                1L,
                10L,
                EventType.COMPONENT_RESERVED,
                "RESERVING",
                "RESERVED",
                "Reservation successful",
                null
        );
    }

    @Test
    void recordSavesEventAndReturnsSavedEvent() {
        when(eventRepository.save(any(OrderEvent.class)))
                .thenReturn(event);

        OrderEvent result = eventService.record(
                1L,
                10L,
                EventType.COMPONENT_RESERVED,
                "RESERVING",
                "RESERVED",
                "Reservation successful",
                null
        );

        assertSame(event, result);
        verify(eventRepository).save(any(OrderEvent.class));
    }

    @Test
    void recordPassesCorrectEventDataToRepository() {
        when(eventRepository.save(any(OrderEvent.class)))
                .thenReturn(event);

        eventService.record(
                1L,
                10L,
                EventType.COMPONENT_RESERVED,
                "RESERVING",
                "RESERVED",
                "Reservation successful",
                null
        );

        verify(eventRepository).save(argThat(savedEvent ->
                savedEvent.getOrderId().equals(1L)
                        && savedEvent.getComponentId().equals(10L)
                        && savedEvent.getEventType() == EventType.COMPONENT_RESERVED
                        && savedEvent.getFromStatus().equals("RESERVING")
                        && savedEvent.getToStatus().equals("RESERVED")
                        && savedEvent.getReason().equals("Reservation successful")
        ));
    }

    @Test
    void getOrderEventsReturnsEventsFromRepository() {
        List<OrderEvent> events = List.of(event);

        when(eventRepository.findByOrderIdOrderByIdAsc(1L))
                .thenReturn(events);

        List<OrderEvent> result = eventService.getOrderEvents(1L);

        assertSame(events, result);
        verify(eventRepository).findByOrderIdOrderByIdAsc(1L);
    }

    @Test
    void getOrderEventsReturnsEmptyListWhenNoEventsExist() {
        when(eventRepository.findByOrderIdOrderByIdAsc(1L))
                .thenReturn(List.of());

        List<OrderEvent> result = eventService.getOrderEvents(1L);

        assertEquals(0, result.size());
    }

    @Test
    void getComponentEventsReturnsEventsFromRepository() {
        List<OrderEvent> events = List.of(event);

        when(eventRepository
                .findByOrderIdAndComponentIdOrderByIdAsc(1L, 10L))
                .thenReturn(events);

        List<OrderEvent> result =
                eventService.getComponentEvents(1L, 10L);

        assertSame(events, result);

        verify(eventRepository)
                .findByOrderIdAndComponentIdOrderByIdAsc(1L, 10L);
    }

    @Test
    void getComponentEventsReturnsEmptyListWhenNoEventsExist() {
        when(eventRepository
                .findByOrderIdAndComponentIdOrderByIdAsc(1L, 10L))
                .thenReturn(List.of());

        List<OrderEvent> result =
                eventService.getComponentEvents(1L, 10L);

        assertEquals(0, result.size());
    }
}
