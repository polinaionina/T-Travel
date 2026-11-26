package ru.ttravel.contracts.supplier.response;

import ru.ttravel.contracts.supplier.common.SupplierBookingStatus;

/* Ответ на запрос текущего состояния бронирования. */
public record BookingStatusResponse(
        SupplierBookingStatus status,
        String externalBookingId
) {
}