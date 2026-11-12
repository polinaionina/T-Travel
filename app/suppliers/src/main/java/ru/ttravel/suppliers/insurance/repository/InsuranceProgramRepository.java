package ru.ttravel.suppliers.insurance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ttravel.suppliers.insurance.domain.InsuranceProgram;

public interface InsuranceProgramRepository extends JpaRepository<InsuranceProgram, Integer> {
}