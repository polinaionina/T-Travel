package ru.ttravel.suppliers.airline.generator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ru.ttravel.suppliers.airline.domain.*;
import ru.ttravel.suppliers.airline.repository.*

/**
 * Fills the airline simulator with deterministic test data on startup.
 * Runs only when the database has no flights yet.
 */
@Component
public class AirlineDataGenerator implements ApplicationRunner{

    private record Route(String from, String to, int durationMin, long basePrice) {}

    private static final List<Route> ROUTES = List.of(
            new Route("SVO", "AER", 175, 900000),
            new Route("SVO", "LED", 85, 500000),
            new Route("SVO", "KZN", 90, 550000),
            new Route("SVO", "KGD", 130, 800000),
            new Route("LED", "AER", 200, 1000000),
            new Route("SVO", "SVX", 150, 950000));
    
    private static final int[] DEPARTURE_HOURS = {6, 8, 12, 16};

    private final AirportRepository airports;
    private final AircraftRepository aircrafts;
    private final FlightRepository flights;
    private final FlightFareRepository fares;

    private final long seed;
    private final int daysAhead;

    public AirlineDataGenerator(AirportRepository airports, AircraftRepository aircrafts,
                                FlightRepository flights, FlightFareRepository fares,
                                @Value("${simulator.airline.seed:67}") long seed,
                                @Value("${simulator.airline.days-ahead:90}") int daysAhead) {
        this.airports = airports;
        this.aircrafts = aircrafts;
        this.flights = flights;
        this.fares = fares;
        this.seed = seed;
        this.daysAhead = daysAhead;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (flights.count() > 0) {
            return;
        }
        Random random = new Random(seed);
        seedReferenceData();
        generateFlights(random);
    }

    private void seedReferenceData() {
        airports.saveAll(List.of(
                new Airport("SVO", "Moscow", "Sheremetyevo", "Europe/Moscow"),
                new Airport("LED", "Saint Petersburg", "Pulkovo", "Europe/Moscow"),
                new Airport("AER", "Sochi", "Sochi International", "Europe/Moscow"),
                new Airport("KZN", "Kazan", "Kazan International", "Europe/Moscow"),
                new Airport("KGD", "Kaliningrad", "Khrabrovo", "Europe/Kaliningrad"),
                new Airport("SVX", "Yekaterinburg", "Koltsovo", "Asia/Yekaterinburg")));

        aircrafts.saveAll(List.of(
                new Aircraft("320", "Airbus A320", 180),
                new Aircraft("321", "Airbus A321", 220),
                new Aircraft("73H", "Boeing 737-800", 189)));
    }

    private void generateFlights(Random random) {
        List<Aircraft> fleet = aircrafts.findAll();
        LocalDate startDate = LocalDate.now();
        int flightCounter = 1000;

        List<Flight> batch = new ArrayList<>();
        for (Route route : ROUTES) {
            for (boolean reverse : new boolean[]{false, true}) {
                Airport from = airports.getReferenceById(reverse ? route.to() : route.from());
                Airport to = airports.getReferenceById(reverse ? route.from() : route.to());
                ZoneId zone = ZoneId.of(from.getTimezone());

                for (int hour : DEPARTURE_HOURS) {
                    String flightNo = "SU" + (flightCounter++);
                    Aircraft aircraft = fleet.get(random.nextInt(fleet.size()));

                    for (int day = 0; day < daysAhead; day++) {
                        OffsetDateTime departure = ZonedDateTime
                                .of(startDate.plusDays(day), LocalTime.of(hour, 0), zone)
                                .toOffsetDateTime();
                        OffsetDateTime arrival = departure.plusMinutes(route.durationMin());

                        batch.add(new Flight(flightNo, from, to, departure, arrival,
                                aircraft, "SCHEDULED"));
                    }
                }
            }
        }
        flights.saveAll(batch);

        List<FlightFare> fareBatch = new ArrayList<>();
        for (Flight flight : batch) {
            fareBatch.addAll(buildFares(flight, route(flight), random));
        }
        fares.saveAll(fareBatch);
    }

    private List<FlightFare> buildFares(Flight flight, Route route, Random random) {
        int total = flight.getAircraft().getSeatsTotal();
        int businessSeats = total / 10;
        int economySeats = total - businessSeats;

        long economyPrice = jitter(route.basePrice(), random);
        long businessPrice = economyPrice * 3;

        return List.of(
                new FlightFare(flight, "ECONOMY", economyPrice, random.nextInt(economySeats + 1)),
                new FlightFare(flight, "BUSINESS", businessPrice, random.nextInt(businessSeats + 1)));
    }

    private long jitter(long base, Random random) {
        double factor = 0.85 + random.nextDouble() * 0.30;
        return Math.round(base * factor / 10000.0) * 10000;
    }

    private Route route(Flight flight) {
        String a = flight.getDepartureAirport().getCode();
        String b = flight.getArrivalAirport().getCode();
        return ROUTES.stream()
                .filter(r -> (r.from().equals(a) && r.to().equals(b))
                          || (r.from().equals(b) && r.to().equals(a)))
                .findFirst().orElseThrow();
    }
}
