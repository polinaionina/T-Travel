package ru.ttravel.contracts.supplier.response;

import ru.ttravel.contracts.supplier.models.SupplierError;
import ru.ttravel.contracts.supplier.models.SupplierOperationStatus;

/* Ответ на резервирование. */
public record ReserveResponse(
        SupplierOperationStatus status,
        String externalBookingId,
        Long price,
        String currency,
        SupplierError error
) {
}