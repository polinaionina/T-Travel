package ru.ttravel.orchestrator.domain.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ttravel.orchestrator.domain.data.Notification;
import ru.ttravel.orchestrator.domain.models.NotificationStatus;

import java.util.List;
import java.util.Optional;

/**
 * Доступ к таблице {@code notification}: уведомления пользователям.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Возвращает все уведомления пользователя, новые первыми.
     *
     * @param userId идентификатор пользователя
     * @return уведомления
     */
    List<Notification> findByUserIdOrderByIdDesc(Long userId);

    /**
     * Возвращает уведомления пользователя в заданном статусе, новые первыми.
     * <p>
     * Для списка непрочитанных передаётся {@link NotificationStatus#UNREAD}.
     *
     * @param userId идентификатор пользователя
     * @param status нужный статус уведомления
     * @return уведомления
     */
    List<Notification> findByUserIdAndStatusOrderByIdDesc(Long userId, NotificationStatus status);

    /**
     * Ищет уведомление по идентификатору и идентификатору владельца.
     *
     * @param id     идентификатор уведомления
     * @param userId идентификатор пользователя
     * @return найденное уведомление
     */
    Optional<Notification> findByIdAndUserId(Long id, Long userId);
}