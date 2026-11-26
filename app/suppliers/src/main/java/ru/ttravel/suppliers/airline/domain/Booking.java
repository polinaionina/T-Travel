package ru.ttravel.suppliers.airline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import ru.ttravel.suppliers.models.BookingStatus;

import java.lang.annotation.Inherited;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Booking a flight ticket in the airline simulator.
 * <p>
 * It is created upon successful processing of {@code POST /bookings}. 
 * A repeated request
 * with the same {@code requestId} does not create a new booking but returns the existing one,
 * which is why a uniqueness constraint is imposed on {@code requestId}.
 * <p>
 * The status and cancellation time are changed only together, via {@link #cancel()}.
 */
@Entity
@Table(name = "booking")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The fare for the flight that the reservation is for. The flight is available via {@code fare.getFlight()}*/
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fare_id", referencedColumnName = "id", nullable = false)
    private FlightFare fare;

    /** The identifier of the proposal for which the request was received. A copy for debugging and reconciliation with the orchestrator. */
    @Column(name = "offer_id", nullable = false)
    private String offerId;

    /** The idempotency key sent by the orchestrator. Is unique within the table. */
    @Column(name = "request_id", nullable = false, unique = true)
    private String requestId;

    /** The status of the booking. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status;

    /** The actual price at the time of booking. */
    @Column(name = "price", nullable = false)
    private Long price;

    /** The moment the booking is created in UTC. It is set in the constructor and cannot be changed. */
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /** The cancellation moment in UTC. {@code null} until the reservation is cancelled. */
    @Column(name = "cancelled_at", nullable = true)
    private OffsetDateTime cancelledAt;

    /** Empty constructor, required by Hibernate. */
    protected Booking() {
    }

    /**
     * Creates a new reservation and records the creation time.
     *
     * @param fare      the selected flight fare
     * @param offerId   the identifier of the proposal for which the request was received
     * @param requestId the idempotency key sent by the orchestrator
     * @param status    the initial status, usually {@code CONFIRMED}
     * @param price     the actual price at the time of booking
     */
    public Booking(FlightFare fare, String offerId, String requestId,
                   BookingStatus status, Long price) {
        this.fare = fare;
        this.offerId = offerId;
        this.requestId = requestId;
        this.status = status;
        this.price = price;
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FlightFare getFare() {
        return fare;
    }

    public void setFare(FlightFare fare) {
        this.fare = fare;
    }

    public String getOfferId() {
        return offerId;
    }

    public void setOfferId(String offerId) {
        this.offerId = offerId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void confirm() {
        if (status == BookingStatus.CONFIRMED) return;
        if (status != BookingStatus.RESERVED) {
            throw new IllegalStateException("Cannot confirm booking in status " + status);
        }
        status = BookingStatus.CONFIRMED;
    }

    public void cancel() {
        if (status == BookingStatus.CANCELLED) return;
        status = BookingStatus.CANCELLED;
        cancelledAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}

