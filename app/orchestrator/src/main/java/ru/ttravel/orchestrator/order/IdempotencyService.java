package ru.ttravel.orchestrator.order;

import org.springframework.stereotype.Service;

import ru.ttravel.orchestrator.domain.data.TripOrder;
import ru.ttravel.orchestrator.domain.exceptions.IdempotencyConflictException;
import ru.ttravel.orchestrator.domain.repositories.OrderRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Сервис для обеспечения идемпотентности операций с заказами.
 */
@Service
public class IdempotencyService {

    private final OrderRepository orderRepository;

    public IdempotencyService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Вычисляет хеш переданных данных запроса.
     * Результат возвращается в шестнадцатеричном формате
     * и содержит 64 символа.
     *
     * @param requestData данные запроса в виде строки
     * @return SHA-256 хеш данных запроса
     * @throws IllegalStateException если алгоритм SHA-256 недоступен
     */
    public String calculateHash(String requestData) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(
                    requestData.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashBytes);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    /**
     * Ищет ранее созданный заказ по идентификатору пользователя
     * и ключу идемпотентности.
     *
     * @param userId идентификатор пользователя
     * @param idempotencyKey ключ идемпотентности запроса
     * @return Optional с найденным заказом или пустой Optional,
     *         если заказ не найден
     */
    public Optional<TripOrder> findExistingOrder(
            Long userId,
            String idempotencyKey
    ) {
        return orderRepository.findByUserIdAndIdempotencyKey(
                userId,
                idempotencyKey
        );
    }

    /**
     * Проверяет, совпадает ли хеш нового запроса с хешем
     * первоначального запроса, сохранённым в заказе.
     *
     * @param existingOrder ранее созданный заказ
     * @param requestHash хеш нового запроса
     * @return true, если хеши совпадают, иначе false
     */
    public boolean isSameRequest(TripOrder existingOrder, String requestHash) {
        return existingOrder.getRequestHash().equals(requestHash);
    }

    /**
     * Проверяет повторный запрос на создание заказа.
     *
     * @param userId идентификатор пользователя
     * @param idempotencyKey ключ идемпотентности
     * @param requestHash хеш нового запроса
     * @return найденный заказ или пустой Optional
     * @throws IdempotencyConflictException если ключ использован
     *         с другим содержимым запроса
     */
    public Optional<TripOrder> checkIdempotency(
            Long userId,
            String idempotencyKey,
            String requestHash
    ) throws IdempotencyConflictException {

        Optional<TripOrder> existingOrder =
                findExistingOrder(userId, idempotencyKey);

        if (existingOrder.isEmpty()) {
            return Optional.empty();
        }

        TripOrder order = existingOrder.get();

        if (!isSameRequest(order, requestHash)) {
            throw new IdempotencyConflictException(idempotencyKey);
        }

        return existingOrder;
    }
}
