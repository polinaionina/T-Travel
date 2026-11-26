package ru.ttravel.orchestrator.events;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.ttravel.orchestrator.domain.data.OrderEvent;
import ru.ttravel.orchestrator.domain.models.EventType;
import ru.ttravel.orchestrator.domain.repositories.OrderEventRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class EventService {

    private final OrderEventRepository eventRepository;

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

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

        log.debug("Event recorded: orderId={}, componentId={}, eventType={}, fromStatus={}, toStatus={}",
                orderId,
                componentId,
                eventType,
                fromStatus,
                toStatus
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
