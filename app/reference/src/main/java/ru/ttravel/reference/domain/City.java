package ru.ttravel.reference.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A city in the reference service: the single source of truth for cities.
 * <p>
 * Other services store only {@link #getId()} and never copy names or
 * coordinates. Records are never deleted and ids are never reused, because
 * other databases refer to them without a foreign key.
 */
@Entity
@Table(name = "city")
public class City {

    /** Stable numeric id, assigned manually in the migration so it is the same in every environment. */
    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    /** Short unique code for people and debugging, e.g. {@code MOW}. */
    @Column(name = "code", length = 8, nullable = false, unique = true)
    private String code;

    /** Display name, e.g. {@code Moscow}. */
    @Column(name = "name", nullable = false)
    private String name;

    /** ISO 3166-1 alpha-2 country code, e.g. {@code RU}. */
    @Column(name = "country_code", length = 2, nullable = false)
    private String countryCode;

    /** IANA time zone name, e.g. {@code Europe/Moscow}. Not an offset like {@code UTC+3}. */
    @Column(name = "timezone", length = 64, nullable = false)
    private String timezone;

    /** Latitude of the city centre in degrees. */
    @Column(name = "lat", nullable = true)
    private Double lat;

    /** Longitude of the city centre in degrees. */
    @Column(name = "lon", nullable = true)
    private Double lon;

    /** Empty constructor, required by Hibernate. */
    protected City() {
    }

    /**
     * Creates a city with an explicit id.
     *
     * @param id          stable numeric id
     * @param code        short unique code
     * @param name        display name
     * @param countryCode ISO 3166-1 alpha-2 country code
     * @param timezone    IANA time zone name
     * @param lat         latitude, may be {@code null}
     * @param lon         longitude, may be {@code null}
     */
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