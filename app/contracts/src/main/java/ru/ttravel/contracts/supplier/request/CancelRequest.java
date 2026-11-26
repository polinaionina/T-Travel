package ru.ttravel.contracts.supplier.request;

/* Запрос на отмену бронирования. */
public record CancelRequest(
        Long orderId,
        Long componentId,
        String externalBookingId,
        String requestId
) {
}