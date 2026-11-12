package ru.ttravel.orchestrator.domain.exceptions;

/**
 * Уведомление с указанным идентификатором не найдено (HTTP 404).
 */
public class NotificationNotFoundException extends OrchestratorException {

    /**
     * @param notificationId идентификатор уведомления, которого нет
     */
    public NotificationNotFoundException(Long notificationId) {
        super("NOTIFICATION_NOT_FOUND", "Уведомление не найдено: id=" + notificationId);
    }
}