package ru.ttravel.contracts.supplier.response;

import ru.ttravel.contracts.supplier.models.SupplierError;
import ru.ttravel.contracts.supplier.models.SupplierOperationStatus;

/* Ответ на подтверждение бронирования. */
public record ConfirmResponse(
        SupplierOperationStatus status,
        String externalBookingId,
        SupplierError error
) {
}