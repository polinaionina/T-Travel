package ru.ttravel.contracts.supplier.request;

/* Запрос на проверку состояния бронирования. */
public record BookingStatusRequest(
        Long orderId,
        Long componentId,
        String externalBookingId,
        String requestId
) {
}