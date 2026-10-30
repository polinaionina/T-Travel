package ru.ttravel.orchestrator.domain.exceptions;

/**
 * Заказ с указанным идентификатором не найден (HTTP 404).
 */
public class OrderNotFoundException extends OrchestratorException {

    /**
     * @param orderId идентификатор заказа, которого нет
     */
    public OrderNotFoundException(Long orderId) {
        super("ORDER_NOT_FOUND", "Заказ не найден: id=" + orderId);
    }
}