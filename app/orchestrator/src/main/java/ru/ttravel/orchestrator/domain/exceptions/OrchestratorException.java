package ru.ttravel.orchestrator.domain.exceptions;

/**
 * Базовое исключение оркестратора.
 * <p>
 * Несёт машинный код ошибки ({@code ORDER_NOT_FOUND} и т. п.), по которому
 * слой {@code api} формирует ответ и выбирает HTTP-статус.
 */
public abstract class OrchestratorException extends Exception {

    private final String code;

    /**
     * @param code    машинный код ошибки
     * @param message сообщение для человека
     */
    protected OrchestratorException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * @param code    машинный код ошибки
     * @param message сообщение для человека
     * @param cause   исходная причина
     */
    protected OrchestratorException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * @return машинный код ошибки
     */
    public String getCode() {
        return code;
    }
}