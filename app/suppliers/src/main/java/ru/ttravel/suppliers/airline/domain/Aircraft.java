package ru.ttravel.suppliers.airline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The essence of an aircraft
 */
@Entity
@Table(name = "aircraft")
public class Aircraft {
    @Id
    @Column(name = "code", length = 3, nullable = false)
    private String code;

    @Column(name = "model", nullable = false)
    private String model;

    @Column(name = "seats_total", nullable = false)
    private Integer seatsTotal;

    /** Empty constructor, required by Hibernate. */
    protected Aircraft() {
    }

    /**
     * Creates a new reservation and records the creation time.
     *
     * @param code      aircraft code, usually IATA code
     * @param model     aircraft model, usually IATA code
     * @param seatsTotal total count of seats in the aircraft
     */
    public Aircraft(String code, String model, Integer seatsTotal) {
        this.code = code;
        this.model = model;
        this.seatsTotal = seatsTotal;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getSeatsTotal() {
        return seatsTotal;
    }

    public void setSeatsTotal(Integer seatsTotal) {
        this.seatsTotal = seatsTotal;
    }
}