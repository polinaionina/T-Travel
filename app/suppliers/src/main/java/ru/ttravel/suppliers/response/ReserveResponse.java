package ru.ttravel.contracts.supplier.response;

import java.time.Instant;

import ru.ttravel.contracts.supplier.models.SupplierBookingStatus;
import ru.ttravel.contracts.supplier.models.SupplierError;
import ru.ttravel.contracts.supplier.models.SupplierOperationStatus;

public record ReserveResponse(
        SupplierOperationStatus operationStatus,
        SupplierBookingStatus bookingStatus,
        Long externalBookingId,
        Long price,
        String currency,
        Instant expiresAt,
        SupplierError error
) {

    public static ReserveResponse success(Long externalBookingId, Long price,
                                          String currency, Instant expiresAt) {
        return new ReserveResponse(SupplierOperationStatus.SUCCESS, SupplierBookingStatus.RESERVED,
                externalBookingId, price, currency, expiresAt, null);
    }

    public static ReserveResponse failed(SupplierError error) {
        return new ReserveResponse(SupplierOperationStatus.FAILED, null,
                null, null, null, null, error);
    }
}