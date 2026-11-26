package ru.ttravel.orchestrator.domain.models;

public enum OrderStatus {
    DRAFT, 
    CONFIRMING,
    PAYMENT_PENDING,
    CONFIRMED,
    ACTION_REQUIRED,
    CANCELLED,
    CANCELLING,
    PAYMENT_FAILED,
    NEEDS_HELP;

    /**
     * Можно ли в этом статусе менять состав заказа (выбирать предложения),
     * отправлять его на подтверждение и отменять.
     *
     * @return {@code true} для {@link #DRAFT} и {@link #ACTION_REQUIRED}
     */
    public boolean isEditable() {
        return this == DRAFT || this == ACTION_REQUIRED;
    }
}