package ru.ttravel.suppliers.hotel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Availability and price of a {@link RoomType} for one night.
 * <p>
 * One row means: "for the night starting on {@code date}, {@code roomsAvailable}
 * rooms of this type are free". It is not tied to any booking. This table is the
 * source of truth for selling rooms: search, booking and cancellation all work
 * through it, night by night.
 * <p>
 * A stay is possible only if {@code roomsAvailable > 0} for every night from
 * {@code checkIn} (inclusive) to {@code checkOut} (exclusive). Booking decreases
 * the counter by one for each night, cancellation increases it back.
 */
@Entity
@Table(name = "room_availability",
        uniqueConstraints = @UniqueConstraint(columnNames = {"room_type_id", "date"}))
public class RoomAvailability {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", referencedColumnName = "id", nullable = false)
    private RoomType roomType;

    /** The night, as a calendar date without time or time zone. Unique per room type. */
    @Column(name = "date", nullable = false)
    private LocalDate date;

    /** Number of free rooms for this night. Never negative. */
    @Column(name = "rooms_available", nullable = false)
    private Integer roomsAvailable;

    /** Price for this night as an integer amount in the project-wide unit. May differ by date (weekends, season). */
    @Column(name = "price", nullable = false)
    private Long price;

    /** Empty constructor, required by Hibernate. */
    protected RoomAvailability() {
    }

    /**
     * Creates an availability record for one night.
     *
     * @param roomType       room type
     * @param date           the night
     * @param roomsAvailable number of free rooms
     * @param price          price for the night
     */
    public RoomAvailability(RoomType roomType, LocalDate date, Integer roomsAvailable, Long price) {
        this.roomType = roomType;
        this.date = date;
        this.roomsAvailable = roomsAvailable;
        this.price = price;
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

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getRoomsAvailable() {
        return roomsAvailable;
    }

    public void setRoomsAvailable(Integer roomsAvailable) {
        this.roomsAvailable = roomsAvailable;
    }

    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
    }
}
