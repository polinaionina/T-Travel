package ru.ttravel.orchestrator.domain.exceptions;

/**
 * Заказ изменили параллельно: не совпала версия при сохранении (HTTP 409).
 * Запрос можно безопасно повторить.
 */
public class ConcurrentOrderModificationException extends OrchestratorException {

    /**
     * @param orderId идентификатор заказа
     * @param cause   исходное исключение оптимистичной блокировки
     */
    public ConcurrentOrderModificationException(Long orderId, Throwable cause) {
        super("CONCURRENT_MODIFICATION",
                "Заказ изменён параллельно, повторите запрос: id=" + orderId, cause);
    }
}