package ru.ttravel.reference.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ttravel.reference.domain.City;

public interface CityRepository extends JpaRepository<City, Integer> {
}