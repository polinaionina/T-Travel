package ru.ttravel.orchestrator.recovery;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.ttravel.orchestrator.domain.data.TripOrder;
import ru.ttravel.orchestrator.domain.models.OrderStatus;
import ru.ttravel.orchestrator.domain.repositories.OrderRepository;

import java.util.List;
import java.util.Set;

/**
 * Находит незавершённые заказы для восстановления после сбоя
 * или перезапуска оркестратора.
 */
@Service
public class RecoveryService {

    private static final Set<OrderStatus> RECOVERABLE_STATUSES = Set.of(
            OrderStatus.SUBMITTED,
            OrderStatus.CONFIRMING,
            OrderStatus.COMPENSATING
    );

    private final OrderRepository orderRepository;

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
        return orderRepository.findByStatusIn(RECOVERABLE_STATUSES);
    }

    /**
     * Находит заказы, ожидающие продолжения подтверждения.
     *
     * @return заказы в статусах SUBMITTED и CONFIRMING
     */
    @Transactional(readOnly = true)
    public List<TripOrder> findOrdersForConfirmationRecovery() {
        return orderRepository.findByStatusIn(Set.of(
                OrderStatus.SUBMITTED,
                OrderStatus.CONFIRMING
        ));
    }

    /**
     * Находит заказы, для которых необходимо продолжить компенсацию.
     *
     * @return заказы в статусе COMPENSATING
     */
    @Transactional(readOnly = true)
    public List<TripOrder> findOrdersForCompensationRecovery() {
        return orderRepository.findByStatusIn(Set.of(
                OrderStatus.COMPENSATING
        ));
    }
}