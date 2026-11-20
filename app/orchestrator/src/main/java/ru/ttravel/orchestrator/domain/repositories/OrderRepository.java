package ru.ttravel.orchestrator.domain.repositories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import ru.ttravel.orchestrator.domain.data.TripOrder;
import ru.ttravel.orchestrator.domain.models.OrderStatus;

import org.springframework.data.domain.*;

import java.time.Instant;
import java.util.*;

/**
 * Доступ к таблице {@code trip_order}: составные заказы поездок.
 */
public interface OrderRepository extends JpaRepository<TripOrder, Long> {
    
    /**
     * Находит заказ вместе со списком компонентов одним запросом.
     * 
     * Компоненты в {@link TripOrder} загружаются лениво, поэтому обычный
     * {@code findById} не подходит, если нужно прочитать компоненты вне транзакции
     * (например, при сборке ответа для web).
     *
     * @param id идентификатор заказа
     * @return заказ с компонентами или пустой результат, если заказа нет
     */
    @EntityGraph(attributePaths = "components")
    Optional<TripOrder> findWithComponentsById(Long id);

    /**
     * Ищет заказ по ключу идемпотентности.
     * 
     * Если заказ найден, значит, запрос создания уже выполнялся, и нужно вернуть этот заказ,
     * а не создавать новый.
     *
     * @param userId идентификатор пользователя
     * @param idempotencyKey значение заголовка {@code Idempotency-Key}
     * @return заказ или пустой результат, если такого запроса ещё не было
     */
    Optional<TripOrder> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    /**
     * Находит все заказы в одном из указанных статусов.
     * 
     * Используется при перезапуске оркестратора, чтобы найти незавершённые заказы
     * ({@code SUBMITTED}, {@code CONFIRMING}, {@code COMPENSATING}) и продолжить процесс.
     *
     * @param statuses набор статусов
     * @return подходящие заказы, возможно пустой список
     */
    List<TripOrder> findByStatusIn(Collection<OrderStatus> statuses);

    /**
     * Возвращает страницу заказов пользователя.
     *
     * @param userId   идентификатор пользователя
     * @param pageable номер страницы, размер и сортировка
     * @return страница заказов
     */
    Page<TripOrder> findByUserId(Long userId, Pageable pageable);

    /**
     * Возвращает страницу заказов пользователя в заданном статусе.
     *
     * @param userId   идентификатор пользователя
     * @param status   нужный статус заказа
     * @param pageable номер страницы, размер и сортировка
     * @return страница заказов
     */
    Page<TripOrder> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);

    /**
     * Находит заказы в указанных статусах, с которыми давно не работали.
     * 
     * Используется {@code TimeoutWatcher} для отката заброшенных заказов
     * ({@code DRAFT} и {@code ACTION_REQUIRED}).
     *
     * @param statuses набор статусов
     * @param moment момент, раньше которого заказ последний раз обновлялся
     * @return заброшенные заказы
     */
    List<TripOrder> findByStatusInAndUpdatedAtBefore(Collection<OrderStatus> statuses, Instant moment);
}
