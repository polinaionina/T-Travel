package ru.ttravel.contracts.supplier.response;

import ru.ttravel.contracts.supplier.common.SupplierError;
import ru.ttravel.contracts.supplier.common.SupplierOperationStatus;

/* Ответ на подтверждение бронирования. */
public record ConfirmResponse(
        SupplierOperationStatus status,
        String externalBookingId,
        SupplierError error
) {
}