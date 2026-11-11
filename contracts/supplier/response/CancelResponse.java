package ru.ttravel.contracts.supplier.response;

import ru.ttravel.contracts.supplier.common.SupplierError;
import ru.ttravel.contracts.supplier.common.SupplierOperationStatus;

/* Ответ на отмену бронирования. */
public record CancelResponse(
        SupplierOperationStatus status,
        SupplierError error
) {
}