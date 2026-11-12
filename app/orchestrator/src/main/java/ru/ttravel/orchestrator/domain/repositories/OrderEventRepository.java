package ru.ttravel.orchestrator.domain.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ttravel.orchestrator.domain.data.OrderEvent;

import java.util.List;

/**
 * Доступ к таблице {@code order_event}: журнал событий заказа.
 * <p>
 * Журнал только пополняется. Записи сортируются по {@code id}, потому что он
 * растёт монотонно, а время создания у двух событий может совпасть.
 */
public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {

    /**
     * Возвращает все события заказа в порядке возникновения (для временной шкалы).
     *
     * @param orderId идентификатор заказа
     * @return события от первого к последнему
     */
    List<OrderEvent> findByOrderIdOrderByIdAsc(Long orderId);

    /**
     * Возвращает события одного компонента заказа в порядке возникновения.
     *
     * @param orderId     идентификатор заказа
     * @param componentId идентификатор компонента
     * @return события от первого к последнему
     */
    List<OrderEvent> findByOrderIdAndComponentIdOrderByIdAsc(Long orderId, Long componentId);
}