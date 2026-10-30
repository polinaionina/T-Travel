package ru.ttravel.orchestrator.domain.models;

public enum OrderStatus {
    DRAFT, 
    SUBMITTED, 
    CONFIRMING, 
    CONFIRMED,
    ACTION_REQUIRED,
    COMPENSATING, 
    CANCELLED, 
    COMPENSATION_FAILED;

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