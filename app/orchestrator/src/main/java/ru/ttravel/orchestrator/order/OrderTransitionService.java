package ru.ttravel.orchestrator.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ttravel.orchestrator.domain.data.TripOrder;
import ru.ttravel.orchestrator.domain.exceptions.InvalidStatusTransitionException;
import ru.ttravel.orchestrator.domain.models.EventType;
import ru.ttravel.orchestrator.domain.models.OrderStatus;
import ru.ttravel.orchestrator.domain.repositories.OrderRepository;
import ru.ttravel.orchestrator.domain.state.OrderStateMachine;
import ru.ttravel.orchestrator.events.EventService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Сервис управления переходами заказов между статусами.
 */
@Service
public class OrderTransitionService {

    private final OrderStateMachine stateMachine;
    private final OrderRepository orderRepository;
    private final EventService eventService;

    private static final Logger log = LoggerFactory.getLogger(OrderTransitionService.class);

    public OrderTransitionService(
            OrderStateMachine stateMachine,
            OrderRepository orderRepository,
            EventService eventService
    ) {
        this.stateMachine = stateMachine;
        this.orderRepository = orderRepository;
        this.eventService = eventService;
    }

    /**
     * Переводит заказ в новый статус и записывает событие перехода.
     *
     * @param order     заказ, статус которого необходимо изменить
     * @param newStatus целевой статус
     * @param eventType тип события для журнала
     * @param reason    причина перехода
     * @throws InvalidStatusTransitionException если переход запрещён
     */
    @Transactional
    public void transition(
            TripOrder order,
            OrderStatus newStatus,
            EventType eventType,
            String reason
    ) throws InvalidStatusTransitionException {

        OrderStatus previousStatus = order.getStatus();

        try {
            stateMachine.assertAllowed(previousStatus, newStatus);
        } catch (InvalidStatusTransitionException e) {
            log.warn(
                    "Invalid order status transition: orderId={}, previousStatus={}, newStatus={}",
                    order.getId(),
                    previousStatus,
                    newStatus
            );
            throw e;
        }

        order.changeStatus(newStatus);

        log.info("Order status changed: orderId={}, previousStatus={}, newStatus={}, eventType={}",
                order.getId(),
                previousStatus,
                newStatus,
                eventType
        );

        eventService.record(
                order.getId(),
                null,
                eventType,
                previousStatus.name(),
                newStatus.name(),
                reason,
                null
        );

        orderRepository.save(order);
    }
}