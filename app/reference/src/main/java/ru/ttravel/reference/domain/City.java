package ru.ttravel.reference.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity
@Table(name = "city")
public class City {
    
    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "code", length = 8, nullable = false, unique = true)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "country_code", length = 2, nullable = false)
    private String countryCode;

    @Column(name = "timezone", length = 64, nullable = false)
    private String timezone;

    @Column(name = "lat", nullable = true)
    private Double lat;

    @Column(name = "lon", nullable = true)
    private Double lon;

    protected City() {
    }

    public City(Integer id, String code, String name, String countryCode,
                String timezone, Double lat, Double lon) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.countryCode = countryCode;
        this.timezone = timezone;
        this.lat = lat;
        this.lon = lon;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

}