package ru.ttravel.contracts.supplier.models;

public record SupplierError(
        String code,
        String message
) {
}