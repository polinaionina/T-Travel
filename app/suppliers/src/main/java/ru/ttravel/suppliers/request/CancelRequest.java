package ru.ttravel.contracts.supplier.request;

public record CancelRequest(
        Long externalBookingId,
        String reason
) {
}