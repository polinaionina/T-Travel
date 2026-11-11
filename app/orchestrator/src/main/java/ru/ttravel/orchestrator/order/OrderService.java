package ru.ttravel.orchestrator.order;

import org.springframework.stereotype.Service;

import jakarta.validation.constraints.NotNull;
import ru.ttravel.orchestrator.domain.data.TripOrder;
import ru.ttravel.orchestrator.domain.exceptions.OrchestratorException;
import ru.ttravel.orchestrator.domain.exceptions.OrderNotFoundException;
import ru.ttravel.orchestrator.domain.repositories.OrderRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final IdempotencyService idempotencyService;
    private final OrderTransitionService orderTransitionService;

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
    ) throws OrchestratorException;

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
    ) throws OrderNotFoundException;

    /**
     * Возвращает страницу заказов пользователя.
     *
     * @param userId идентификатор пользователя
     * @param pageable параметры пагинации
     * @return страница заказов
     */
    public Page<TripOrder> getUserOrders(
            @NotNull Long userId,
            @NotNull Pageable pageable
    );

    /**
     * Отменяет заказ пользователя.
     *
     * @param orderId идентификатор заказа
     * @param userId идентификатор пользователя
     */
    public void cancelOrder(
            @NotNull Long orderId,
            @NotNull Long userId
    ) throws OrchestratorException;
}