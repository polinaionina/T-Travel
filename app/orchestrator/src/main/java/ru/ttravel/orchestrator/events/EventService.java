package ru.ttravel.orchestrator.events;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.ttravel.orchestrator.domain.data.OrderEvent;
import ru.ttravel.orchestrator.domain.models.EventType;
import ru.ttravel.orchestrator.domain.repositories.OrderEventRepository;

import java.util.List;

@Service
public class EventService {

    private final OrderEventRepository eventRepository;

    public EventService(OrderEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * Записывает новое событие в журнал заказа.
     */
    @Transactional
    public OrderEvent record(
            Long orderId,
            Long componentId,
            EventType eventType,
            String fromStatus,
            String toStatus,
            String reason,
            String payload
    ) {
        OrderEvent event = new OrderEvent(
                orderId,
                componentId,
                eventType,
                fromStatus,
                toStatus,
                reason,
                payload
        );

        return eventRepository.save(event);
    }

    /**
     * Получает всю историю заказа.
     */
    @Transactional(readOnly = true)
    public List<OrderEvent> getOrderEvents(Long orderId) {
        return eventRepository.findByOrderIdOrderByIdAsc(orderId);
    }

    /**
     * Получает историю конкретного компонента заказа.
     */
    @Transactional(readOnly = true)
    public List<OrderEvent> getComponentEvents(
            Long orderId,
            Long componentId
    ) {
        return eventRepository
                .findByOrderIdAndComponentIdOrderByIdAsc(orderId, componentId);
    }
}
