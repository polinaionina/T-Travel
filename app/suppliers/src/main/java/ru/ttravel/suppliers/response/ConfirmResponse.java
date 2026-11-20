package ru.ttravel.contracts.supplier.response;

import ru.ttravel.contracts.supplier.models.SupplierBookingStatus;
import ru.ttravel.contracts.supplier.models.SupplierError;
import ru.ttravel.contracts.supplier.models.SupplierOperationStatus;

public record ConfirmResponse(
        SupplierOperationStatus operationStatus,
        SupplierBookingStatus bookingStatus,
        Long price,
        String currency,
        SupplierError error
) {

    public static ConfirmResponse success(Long price, String currency) {
        return new ConfirmResponse(SupplierOperationStatus.SUCCESS, SupplierBookingStatus.CONFIRMED,
                price, currency, null);
    }

    public static ConfirmResponse failed(SupplierBookingStatus bookingStatus, SupplierError error) {
        return new ConfirmResponse(SupplierOperationStatus.FAILED, bookingStatus,
                null, null, error);
    }
}