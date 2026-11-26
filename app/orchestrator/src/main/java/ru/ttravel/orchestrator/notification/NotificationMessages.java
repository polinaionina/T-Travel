package ru.ttravel.orchestrator.notification;

/**
 * Формирует тексты уведомлений о событиях заказа.
 */
public final class NotificationMessages {

    private NotificationMessages() {
    }

    /**
     * Сообщение о создании заказа.
     */
    public static String orderCreated(Long orderId) {
        return "Заказ №" + orderId + " создан.";
    }

    /**
     * Сообщение о необходимости действий пользователя.
     */
    public static String actionRequired(Long orderId, String reason) {
        return "Заказ №" + orderId
                + " требует вашего внимания. Причина: " + reason;
    }

    /**
     * Сообщение об успешном подтверждении заказа.
     */
    public static String orderConfirmed(Long orderId) {
        return "Заказ №" + orderId + " успешно подтверждён.";
    }

    /**
     * Сообщение об отмене заказа.
     */
    public static String orderCancelled(Long orderId) {
        return "Заказ №" + orderId + " отменён.";
    }
}