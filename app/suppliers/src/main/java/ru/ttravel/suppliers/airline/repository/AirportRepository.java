package ru.ttravel.suppliers.airline.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.ttravel.suppliers.airline.domain.Airport;
public interface AirportRepository extends JpaRepository<Airport, String> {}