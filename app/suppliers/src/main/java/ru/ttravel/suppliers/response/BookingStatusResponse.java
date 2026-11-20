package ru.ttravel.contracts.supplier.response;

import ru.ttravel.contracts.supplier.models.SupplierBookingStatus;
import ru.ttravel.contracts.supplier.models.SupplierError;

public record BookingStatusResponse(
        SupplierBookingStatus bookingStatus,
        Long externalBookingId,
        SupplierError error
) {

    public static BookingStatusResponse of(SupplierBookingStatus status, Long externalBookingId) {
        return new BookingStatusResponse(status, externalBookingId, null);
    }

    public static BookingStatusResponse notFound(Long externalBookingId) {
        return new BookingStatusResponse(SupplierBookingStatus.NOT_FOUND, externalBookingId, null);
    }
}