package ru.ttravel.orchestrator.domain.exceptions;

/**
 * Некорректные параметры или тело запроса (HTTP 400).
 */
public class InvalidRequestException extends OrchestratorException {

    /**
     * @param message что именно не так с запросом
     */
    public InvalidRequestException(String message) {
        super("VALIDATION_ERROR", message);
    }
}