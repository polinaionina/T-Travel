package ru.ttravel.contracts.supplier.response;

import ru.ttravel.contracts.supplier.models.SupplierError;
import ru.ttravel.contracts.supplier.models.SupplierOperationStatus;

/* Ответ на отмену бронирования. */
public record CancelResponse(
        SupplierOperationStatus status,
        SupplierError error
) {
}