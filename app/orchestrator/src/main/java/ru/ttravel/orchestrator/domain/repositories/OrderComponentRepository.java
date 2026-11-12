package ru.ttravel.orchestrator.domain.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ttravel.orchestrator.domain.data.OrderComponent;
import ru.ttravel.orchestrator.domain.models.ComponentStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Доступ к таблице {@code order_component}: компоненты заказа
 * (рейс, гостиница, страховка, трансфер).
 */
public interface OrderComponentRepository extends JpaRepository<OrderComponent, Long> {

    /**
     * Возвращает компоненты заказа в порядке выполнения шагов.
     * <p>
     * Компоненты с одинаковым {@code stepNo} выполняются параллельно,
     * с большим номером выполняются позже.
     *
     * @param orderId идентификатор заказа
     * @return компоненты, отсортированные по номеру шага
     */
    List<OrderComponent> findByTripOrderIdOrderByStepNoAsc(Long orderId);

    /**
     * Находит компоненты, у которых истёк срок подтверждения.
     * <p>
     * Используется {@code TimeoutWatcher}. Компоненты без срока
     * ({@code deadline_at} пустой) в результат не попадают.
     *
     * @param statuses статусы, в которых компонент считается незавершённым
     * @param moment   момент, раньше которого должен быть срок
     * @return просроченные компоненты
     */
    List<OrderComponent> findByStatusInAndDeadlineAtBefore(Collection<ComponentStatus> statuses, Instant moment);

    /**
     * Находит компоненты заказа в одном из указанных статусов.
     * <p>
     * Нужен при откате (какие компоненты уже зарезервированы или подтверждены)
     * и при восстановлении после рестарта.
     *
     * @param orderId  идентификатор заказа
     * @param statuses набор статусов
     * @return подходящие компоненты
     */
    List<OrderComponent> findByTripOrderIdAndStatusIn(Long orderId, Collection<ComponentStatus> statuses);
}