package ru.ttravel.contracts.supplier.request;

/* Запрос на резервирование предложения. */
public record ReserveRequest(
        Long orderId,
        Long componentId,
        String offerId,
        String requestId
) {
}