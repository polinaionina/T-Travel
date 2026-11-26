package ru.ttravel.orchestrator.domain.exceptions;

/**
 * Ключ идемпотентности уже использован с другим телом запроса (HTTP 409).
 */
public class IdempotencyConflictException extends OrchestratorException {

    /**
     * @param key значение заголовка {@code Idempotency-Key}
     */
    public IdempotencyConflictException(String key) {
        super("IDEMPOTENCY_KEY_CONFLICT",
                "Ключ идемпотентности уже использован с другим запросом: " + key);
    }
}