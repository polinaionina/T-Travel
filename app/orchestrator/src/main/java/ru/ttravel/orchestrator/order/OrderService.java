package ru.ttravel.orchestrator.order;

import org.springframework.stereotype.Service;

import jakarta.validation.constraints.NotNull;
import ru.ttravel.orchestrator.domain.data.TripOrder;
import ru.ttravel.orchestrator.domain.exceptions.InvalidRequestException;
import ru.ttravel.orchestrator.domain.exceptions.OrchestratorException;
import ru.ttravel.orchestrator.domain.exceptions.OrderNotFoundException;
import ru.ttravel.orchestrator.domain.models.ComponentStatus;
import ru.ttravel.orchestrator.domain.models.OrderStatus;
import ru.ttravel.orchestrator.domain.repositories.OrderRepository;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final IdempotencyService idempotencyService;
    private final OrderTransitionService orderTransitionService;

    private static final Set<ComponentStatus> ACTIVE_RESERVATION_STATUSES = Set.of(
            ComponentStatus.RESERVED,
            ComponentStatus.CONFIRMED,
            ComponentStatus.COMPENSATING,
            ComponentStatus.UNKNOWN,
            ComponentStatus.RESERVING
    );

    public OrderService(
            OrderRepository orderRepository,
            IdempotencyService idempotencyService,
            OrderTransitionService orderTransitionService
    ) {
        this.orderRepository = orderRepository;
        this.idempotencyService = idempotencyService;
        this.orderTransitionService = orderTransitionService;
    }

    /**
     * Создаёт новый заказ с проверкой идемпотентности.
     *
     * @param userId идентификатор пользователя
     * @param currency валюта заказа
     * @param idempotencyKey ключ идемпотентности
     * @param requestHash хеш исходного запроса
     * @return созданный или ранее существующий заказ
     */
    public TripOrder createOrder(
            @NotNull Long userId,
            @NotNull String currency,
            @NotNull String idempotencyKey,
            @NotNull String requestHash
    ) throws OrchestratorException {
        var order = idempotencyService.checkIdempotency(userId, idempotencyKey, requestHash);
        if (order.isPresent()) {
            return order.get();
        }
        
        TripOrder newOrder = new TripOrder(
                userId,
                currency,
                idempotencyKey,
                requestHash
        );

        return orderRepository.save(newOrder);
    }

    /**
     * Возвращает заказ пользователя.
     *
     * @param orderId идентификатор заказа
     * @param userId идентификатор пользователя
     * @return найденный заказ
     */
    public TripOrder getOrder(
            @NotNull Long orderId,
            @NotNull Long userId
    ) throws OrderNotFoundException {
        TripOrder tripOrder = orderRepository.findWithComponentsById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!tripOrder.getUserId().equals(userId)) {
            throw new OrderNotFoundException(orderId);
        }

        return tripOrder;
    }

    /**
     * Возвращает страницу заказов пользователя.
     *
     * @param userId идентификатор пользователя
     * @param pageable параметры пагинации
     * @return страница заказов
     */
    @Transactional(readOnly = true)
    public Page<TripOrder> getUserOrders(
            @NotNull Long userId,
            @NotNull Pageable pageable
    ) {
        return orderRepository.findByUserId(userId, pageable);
    }

    /**
     * Отменяет заказ, если для этого не требуется компенсация.
     *
     * Если у заказа есть компоненты, которые уже резервируются,
     * зарезервированы, подтверждены или находятся в неопределённом
     * состоянии, отмена должна выполняться через CompensationService.
     *
     * @param orderId идентификатор заказа
     * @param userId идентификатор пользователя
     * @throws OrchestratorException если заказ нельзя отменить
     */
    @Transactional
    public void cancelOrder(Long orderId, Long userId)
            throws OrchestratorException {

        TripOrder order = getOrder(orderId, userId);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }

        if (order.getStatus() == OrderStatus.COMPENSATING) {
            return;
        }

        boolean hasActiveReservations = order.activeComponents()
                .stream()
                .anyMatch(component ->
                        ACTIVE_RESERVATION_STATUSES.contains(component.getStatus())
                );

        if (hasActiveReservations) {
            throw new InvalidRequestException(
                    "Для отмены заказа требуется компенсация бронирований"
            );
        }

        orderTransitionService.transition(
                order,
                OrderStatus.CANCELLED,
                ru.ttravel.orchestrator.domain.models.EventType.ORDER_CANCELLED,
                "Заказ отменён пользователем"
        );
    }
}