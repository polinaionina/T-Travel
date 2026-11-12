package ru.ttravel.suppliers.insurance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Price multiplier for an age range.
 * <p>
 * Price formula: {@code dailyRate * days * sum of coefficients of all insured people}.
 * For example, a person aged 60-75 may have coefficient 1.80.
 */
@Entity
@Table(name = "age_band")
public class AgeBand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "age_from", nullable = false)
    private Integer ageFrom;

    @Column(name = "age_to", nullable = false)
    private Integer ageTo;

    @Column(name = "coefficient", nullable = false, precision = 4, scale = 2)
    private BigDecimal coefficient;

    /** Empty constructor, required by Hibernate. */
    protected AgeBand() {
    }

    public AgeBand(Integer ageFrom, Integer ageTo, BigDecimal coefficient) {
        this.ageFrom = ageFrom;
        this.ageTo = ageTo;
        this.coefficient = coefficient;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getAgeFrom() { return ageFrom; }
    public void setAgeFrom(Integer ageFrom) { this.ageFrom = ageFrom; }

    public Integer getAgeTo() { return ageTo; }
    public void setAgeTo(Integer ageTo) { this.ageTo = ageTo; }

    public BigDecimal getCoefficient() { return coefficient; }
    public void setCoefficient(BigDecimal coefficient) { this.coefficient = coefficient; }
}