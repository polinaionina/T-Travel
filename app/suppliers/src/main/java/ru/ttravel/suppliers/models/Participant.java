package ru.ttravel.contracts.supplier.models;

import java.time.LocalDate;

public record Participant(
        String fullName,
        LocalDate birthDate
) {
}