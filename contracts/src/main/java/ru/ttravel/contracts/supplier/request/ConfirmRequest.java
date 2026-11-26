package ru.ttravel.contracts.supplier.request;

/* Запрос на подтверждение ранее зарезервированного компонента. */
public record ConfirmRequest(
        Long orderId,
        Long componentId,
        String externalBookingId,
        String requestId
) {
}