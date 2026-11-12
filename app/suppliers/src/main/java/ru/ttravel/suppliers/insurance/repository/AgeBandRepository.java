package ru.ttravel.suppliers.insurance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ttravel.suppliers.insurance.domain.AgeBand;

public interface AgeBandRepository extends JpaRepository<AgeBand, Integer> {
}