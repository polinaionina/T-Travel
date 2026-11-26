package ru.ttravel.contracts.supplier.models;

public record SupplierError(
        SupplierErrorCode code,
        String message
) {}