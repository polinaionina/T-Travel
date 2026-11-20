package ru.ttravel.contracts.supplier.response;

import ru.ttravel.contracts.supplier.models.SupplierBookingStatus;
import ru.ttravel.contracts.supplier.models.SupplierError;
import ru.ttravel.contracts.supplier.models.SupplierOperationStatus;


public record CancelResponse(
        SupplierOperationStatus operationStatus,
        SupplierBookingStatus bookingStatus,
        SupplierError error
) {

    public static CancelResponse success() {
        return new CancelResponse(SupplierOperationStatus.SUCCESS, SupplierBookingStatus.CANCELLED, null);
    }

    public static CancelResponse failed(SupplierBookingStatus bookingStatus, SupplierError error) {
        return new CancelResponse(SupplierOperationStatus.FAILED, bookingStatus, error);
    }
}