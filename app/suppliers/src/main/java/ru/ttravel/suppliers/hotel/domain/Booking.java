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
import main.java.ru.ttravel.suppliers.airline.domain.FlightFare;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "booking")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", referencedColumnName = "id", nullable = false)
    private RoomType roomType;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    @Column(name = "guests", nullable = false)
    private Integer guests;

    @Column(name = "request_id", nullable = false, unique = true)
    private String requestId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "total_price", nullable = false)
    private Long totalPrice;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "cancelled_at", nullable = true)
    private OffsetDateTime cancelledAt;

    protected Booking() {
    }

    public Booking(RoomType roomType, LocalDate checkIn, LocalDate checkOut,
                   Integer guests, String requestId, String status, Long totalPrice) {
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

    public String getStatus() {
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

    /**
     * Cancels the reservation: changes the status to {@code CANCELLED} and records the cancellation time.
     * <p>
     * The operation is idempotent: if the booking has already been cancelled, the method does not change anything.
     * and does not overwrite the time of the first cancellation.
     * <p>
     * The method does not return the seat in {@link FlightFare#getSeatsAvailable()}: this is done by
     * The cancellation service is within the same transaction.
     */
    public void cancel() {
        if ("CANCELLED".equals(status)) {
            return;
        }
        this.status = "CANCELLED";
        this.cancelledAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
