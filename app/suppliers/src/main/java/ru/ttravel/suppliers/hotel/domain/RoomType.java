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

@Entity
@Table(name = "room_type",
        uniqueConstraints = @UniqueConstraint(columnNames = {"hotel_id", "code"})
)
public class RoomType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", referencedColumnName = "id", nullable = false)
    private Hotel hotel;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "base_price", nullable = true)
    private Long basePrice;

    @Column(name = "rooms_total", nullable = true)
    private Long roomTotal;

    protected RoomType() {}

    public RoomType(String name, String code, Integer capacity, 
        Long basePrice, Long roomTotal, Hotel hotel) {
        this.name = name;
        this.code = code;
        this.capacity = capacity;
        this.basePrice = basePrice;
        this.roomTotal = roomTotal;
        this.hotel = hotel;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Long getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(Long basePrice) {
        this.basePrice = basePrice;
    }

    public Long getRoomTotal() {
        return roomTotal;
    }

    public void setRoomTotal(Long roomTotal) {
        this.roomTotal = roomTotal;
    }

    public Hotel getHotel() {
        return hotel;
    }

    public void setHotel(Hotel hotel) {
        this.hotel = hotel;
    }
    
}
