package ru.ttravel.orchestrator.notification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.ttravel.orchestrator.domain.data.Notification;
import ru.ttravel.orchestrator.domain.exceptions.NotificationNotFoundException;
import ru.ttravel.orchestrator.domain.models.NotificationStatus;
import ru.ttravel.orchestrator.domain.repositories.NotificationRepository;

import java.util.List;

/**
 * Сервис управления пользовательскими уведомлениями.
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification createNotification(
            Long userId,
            Long orderId,
            Long eventId,
            String message
    ) {
        Notification notification = new Notification(
                userId,
                orderId,
                eventId,
                message
        );

        return notificationRepository.save(notification);
    }

    /**
     * Возвращает все уведомления пользователя.
     *
     * @param userId идентификатор пользователя
     * @return список уведомлений от новых к старым
     */
    @Transactional(readOnly = true)
    public List<Notification> getAllNotifications(Long userId) {
        return notificationRepository
                .findByUserIdOrderByIdDesc(userId);
    }

    /**
     * Возвращает только непрочитанные уведомления пользователя.
     *
     * @param userId идентификатор пользователя
     * @return список непрочитанных уведомлений
     */
    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository
                .findByUserIdAndStatusOrderByIdDesc(
                        userId,
                        NotificationStatus.UNREAD
                );
    }

    /**
     * Отмечает уведомление как прочитанное.
     *
     * @param notificationId идентификатор уведомления
     * @param userId         идентификатор владельца
     * @throws NotificationNotFoundException если уведомление не найдено
     * или принадлежит другому пользователю
     */
    @Transactional
    public void markAsRead(
            Long notificationId,
            Long userId
    ) throws NotificationNotFoundException {

        Notification notification = notificationRepository
                .findByIdAndUserId(notificationId, userId)
                .orElseThrow(() ->
                        new NotificationNotFoundException(notificationId)
                );

        notification.markRead();
    }
}