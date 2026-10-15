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

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "rooms_available", nullable = false)
    private Integer roomsAvailable;

    @Column(name = "price", nullable = false)
    private Long price;

    protected RoomAvailability() {
    }

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
