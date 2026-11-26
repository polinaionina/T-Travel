package ru.ttravel.suppliers.airline.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ttravel.suppliers.airline.domain.Aircraft;

public interface AircraftRepository extends JpaRepository<Aircraft, String> {}
