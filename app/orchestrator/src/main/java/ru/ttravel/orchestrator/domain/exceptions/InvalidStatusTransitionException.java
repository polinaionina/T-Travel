package ru.ttravel.orchestrator.domain.exceptions;

/**
 * Недопустимый переход статуса, например отмена уже подтверждённого заказа (HTTP 409).
 */
public class InvalidStatusTransitionException extends OrchestratorException {

    /**
     * @param subject что меняет статус: {@code "Заказ"} или {@code "Компонент"}
     * @param from    текущий статус
     * @param to      статус, в который пытались перейти
     */
    public InvalidStatusTransitionException(String subject, Enum<?> from, Enum<?> to) {
        super("INVALID_STATUS_TRANSITION",
                subject + ": переход из " + from + " в " + to + " запрещён");
    }
}