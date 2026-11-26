public interface TicketRepository extends JpaRepository<Ticket, Long> {
    int countByBookingId(Long bookingId);
}