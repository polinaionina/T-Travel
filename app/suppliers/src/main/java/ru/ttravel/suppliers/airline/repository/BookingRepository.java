package ru.ttravel.suppliers.airline.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.ttravel.suppliers.airline.domain.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByRequestId(String requestId);
}