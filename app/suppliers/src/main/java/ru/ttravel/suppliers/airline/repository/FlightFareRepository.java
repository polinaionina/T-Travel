package ru.ttravel.suppliers.airline.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ru.ttravel.suppliers.airline.domain.FlightFare;

public interface FlightFareRepository extends JpaRepository<FlightFare, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update FlightFare f set f.seatsAvailable = f.seatsAvailable - :count "
            + "where f.id = :id and f.seatsAvailable >= :count")
    int takeSeats(@Param("id") Long id, @Param("count") int count);

    
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update FlightFare f set f.seatsAvailable = f.seatsAvailable + :count "
            + "where f.id = :id")
    int releaseSeats(@Param("id") Long id, @Param("count") int count);
}