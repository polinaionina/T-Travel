package ru.ttravel.suppliers.airline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Represents an airport in the system
 */
@Entity
@Table(name = "airport")
public class Airport {
    @Id
    @Column(name = "code", length = 3, nullable = false)
    private String code;

    @Column(name = "city_id", nullable = false)
    private Integer cityId;

    @Column(name = "name", nullable = false)
    private String name;

    /**
     * The timezone where the airport is located
     */
    @Column(name = "timezone", nullable = false)
    private String timezone;

    /** Empty constructor, required by Hibernate. */
    protected Airport() {
    }

    /**
     * Creates a new airport and records the creation time.
     *
     * @param code     airport code, usually IATA code
     * @param cityId   ID of the city where the airport is located
     * @param name     airport name
     * @param timezone timezone where the airport is located
     */
    public Airport(String code, Integer cityId, String name, String timezone) {
        this.code = code;
        this.cityId = cityId;
        this.name = name;
        this.timezone = timezone;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getCityId() {
        return cityId;
    }

    public void setCityId(Integer cityId) {
        this.cityId = cityId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}