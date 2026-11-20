package ru.ttravel.suppliers.insurance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * A person covered by a {@link Policy}. A policy has one or more insured people.
 * <p>
 * Only fictitious data is stored: no real documents or payment details.
 */
@Entity
@Table(name = "insured_person")
public class InsuredPerson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", referencedColumnName = "id", nullable = false)
    private Policy policy;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    /** Empty constructor, required by Hibernate. */
    protected InsuredPerson() {
    }

    public InsuredPerson(Policy policy, String fullName, LocalDate birthDate) {
        this.policy = policy;
        this.fullName = fullName;
        this.birthDate = birthDate;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Policy getPolicy() { return policy; }
    public void setPolicy(Policy policy) { this.policy = policy; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
}