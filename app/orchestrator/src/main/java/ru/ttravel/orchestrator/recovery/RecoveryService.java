package ru.ttravel.orchestrator.recovery;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.ttravel.orchestrator.domain.data.TripOrder;
import ru.ttravel.orchestrator.domain.models.OrderStatus;
import ru.ttravel.orchestrator.domain.repositories.OrderRepository;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Находит незавершённые заказы для восстановления после сбоя
 * или перезапуска оркестратора.
 */
@Service
public class RecoveryService {

    private static final Set<OrderStatus> RECOVERABLE_STATUSES = Set.of(
            OrderStatus.CONFIRMING,
            OrderStatus.PAYMENT_PENDING,
            OrderStatus.CANCELLING
    );

    private final OrderRepository orderRepository;

    private static final Logger log = LoggerFactory.getLogger(RecoveryService.class);

    public RecoveryService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Находит все заказы, обработку которых необходимо восстановить.
     *
     * @return список незавершённых заказов
     */
    @Transactional(readOnly = true)
    public List<TripOrder> findOrdersForRecovery() {
        List<TripOrder> orders = orderRepository.findByStatusIn(RECOVERABLE_STATUSES);

        log.info("Found {} orders for recovery", orders.size());

        return orders;
    }

    /**
     * Находит заказы, ожидающие продолжения подтверждения.
     *
     * @return заказы в статусе CONFIRMING
     */
    @Transactional(readOnly = true)
    public List<TripOrder> findOrdersForConfirmationRecovery() {
        List<TripOrder> orders = orderRepository.findByStatusIn(Set.of(
            OrderStatus.CONFIRMING
        ));

        log.info("Found {} orders for confirmation recovery", orders.size());

        return orders;
    }

    /**
     * Находит заказы, для которых необходимо продолжить компенсацию.
     *
     * @return заказы в статусе CANCELLING
     */
    @Transactional(readOnly = true)
    public List<TripOrder> findOrdersForCompensationRecovery() {
        List<TripOrder> orders = orderRepository.findByStatusIn(Set.of(
            OrderStatus.CANCELLING
        ));

        log.info("Found {} orders for compensation recovery", orders.size());

        return orders;
    }
}