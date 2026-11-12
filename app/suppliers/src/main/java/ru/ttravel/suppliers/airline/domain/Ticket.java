package ru.ttravel.suppliers.airline.domain;

import java.lang.annotation.Inherited;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A passenger's ticket issued under a {@link Booking}.
 * <p>
 * A booking may contain several tickets, one per passenger. The ticket
 * stores only simulated passenger data: no real documents or payment
 * details are kept.
 * <p>
 * Tickets have no status of their own: their state follows the state of the
 * parent booking.
 */
@Entity
@Table(name = "ticket")
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Booking this ticket was issued under. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", referencedColumnName = "id", nullable = false)
    private Booking booking;


    @Column(name = "passenger_name", nullable = false)
    private String passengerName;

    @Column(name = "seat_no", nullable = true)
    private String seatNo;

    /** Empty constructor, required by Hibernate. */
    protected Ticket() {
    }

    /**
     * Creates a ticket for one passenger.
     *
     * @param booking       parent booking
     * @param passengerName passenger name
     * @param seatNo        seat label, or {@code null} if not assigned yet
     */
    public Ticket(Booking booking, String passengerName, String seatNo) {
        this.booking = booking;
        this.passengerName = passengerName;
        this.seatNo = seatNo;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public String getSeatNo() {
        return seatNo;
    }

    public void setSeatNo(String seatNo) {
        this.seatNo = seatNo;
    }

}
