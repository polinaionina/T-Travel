package ru.ttravel.contracts.supplier.request;

import java.util.List;
import java.util.Map;

import ru.ttravel.contracts.supplier.models.Participant;

public record ReserveRequest(
        Long offerId,
        List<Participant> participants,
        Map<String, Object> details
) {
}