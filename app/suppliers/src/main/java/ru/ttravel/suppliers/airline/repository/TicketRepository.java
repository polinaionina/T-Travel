package ru.ttravel.suppliers.airline.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ttravel.suppliers.airline.domain.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    int countByBookingId(Long bookingId);
}