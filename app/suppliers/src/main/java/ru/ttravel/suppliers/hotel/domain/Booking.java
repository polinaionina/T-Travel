package ru.ttravel.suppliers.hotel.domain;

import java.lang.annotation.Inherited;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * A room reservation in the hotel simulator.
 * <p>
 * A booking reserves one room of a given {@link RoomType} for a range of nights.
 * No concrete room number is assigned: rooms of one type are interchangeable.
 * <p>
 * A repeated request with the same {@code requestId} does not create a new
 * booking but returns the existing one, which is why {@code requestId} is unique.
 * <p>
 * The status and the cancellation time are changed only together, via {@link #cancel()}.
 */
@Entity
@Table(name = "booking")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Room type that was reserved. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", referencedColumnName = "id", nullable = false)
    private RoomType roomType;

    /** First night of the stay (inclusive). A calendar date without time zone. */
    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    /**
     * Check-out date (exclusive): this night is not occupied.
     * For example, {@code checkIn = 12.05} and {@code checkOut = 19.05} is 7 nights.
     */
    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    /** Number of guests. Must not exceed {@link RoomType#getCapacity()}. */
    @Column(name = "guests", nullable = false)
    private Integer guests;

    /** Idempotency key sent by the orchestrator. Unique within the table. */
    @Column(name = "request_id", nullable = false, unique = true)
    private String requestId;

    /** Booking state: {@code CONFIRMED} or {@code CANCELLED}. */
    @Column(name = "status", nullable = false)
    private BookingStatus status;

    /** Actual total price for all nights, as an integer amount in the project-wide unit. */
    @Column(name = "total_price", nullable = false)
    private Long totalPrice;

    /** Creation moment in UTC. Set in the constructor and never changed. */
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /** Cancellation moment in UTC. {@code null} until the booking is cancelled. */
    @Column(name = "cancelled_at", nullable = true)
    private OffsetDateTime cancelledAt;

    /** Empty constructor, required by Hibernate. */
    protected Booking() {
    }

    /**
     * Creates a new booking and records the creation time.
     *
     * @param roomType   reserved room type
     * @param checkIn    first night of the stay
     * @param checkOut   check-out date, must be after {@code checkIn}
     * @param guests     number of guests
     * @param requestId  idempotency key from the orchestrator
     * @param status     initial status, usually {@code CONFIRMED}
     * @param totalPrice total price for all nights
     */
    public Booking(RoomType roomType, LocalDate checkIn, LocalDate checkOut,
                   Integer guests, String requestId, BookingStatus status, Long totalPrice) {
        this.roomType = roomType;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.requestId = requestId;
        this.status = status;
        this.totalPrice = totalPrice;
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(LocalDate checkIn) {
        this.checkIn = checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(LocalDate checkOut) {
        this.checkOut = checkOut;
    }

    public Integer getGuests() {
        return guests;
    }

    public void setGuests(Integer guests) {
        this.guests = guests;
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

    public Long getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Long totalPrice) {
        this.totalPrice = totalPrice;
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
