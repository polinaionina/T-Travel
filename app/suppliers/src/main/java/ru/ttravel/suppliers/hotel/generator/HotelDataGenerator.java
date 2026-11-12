package ru.ttravel.suppliers.hotel.generator;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ru.ttravel.suppliers.hotel.domain.Hotel;
import ru.ttravel.suppliers.hotel.domain.RoomAvailability;
import ru.ttravel.suppliers.hotel.domain.RoomType;
import ru.ttravel.suppliers.hotel.repository.HotelRepository;
import ru.ttravel.suppliers.hotel.repository.RoomAvailabilityRepository;
import ru.ttravel.suppliers.hotel.repository.RoomTypeRepository;

/**
 * Fills the hotel simulator with deterministic test data on startup.
 * Runs only when the database has no hotels yet.
 * <p>
 * All prices are in kopecks. City names must match the airline simulator.
 */
@Component
public class HotelDataGenerator implements ApplicationRunner {
    private record City(String name, double lat, double lon) {}

    private record TypeTemplate(String code, String name, int capacity,
                                double priceFactor, int minRooms, int maxRooms) {}

    private static final List<City> CITIES = List.of(
            new City("Moscow", 55.7558, 37.6173),
            new City("Saint Petersburg", 59.9343, 30.3351),
            new City("Sochi", 43.5855, 39.7231),
            new City("Kazan", 55.7961, 49.1064),
            new City("Kaliningrad", 54.7104, 20.4522),
            new City("Yekaterinburg", 56.8389, 60.6057));

    private static final List<TypeTemplate> TYPES = List.of(
            new TypeTemplate("STANDARD", "Standard room", 2, 1.0, 5, 12),
            new TypeTemplate("DELUXE", "Deluxe room", 2, 1.6, 3, 8),
            new TypeTemplate("FAMILY", "Family room", 4, 2.0, 2, 5));
    
    private static final String[] NAME_PREFIXES = {"Grand", "Royal", "Central", "Park", "Imperial"};
    private static final String[] NAME_SUFFIXES = {"Hotel", "Resort", "Palace", "Residence"};
    private static final String[] STREETS = {"Lenina", "Mira", "Pushkina", "Gagarina", "Sovetskaya", "Naberezhnaya"};

    private static final int HOTELS_PER_CITY = 4;

    private static final double SOLD_OUT_NIGHT_PROBABILITY = 0.03;

    private final HotelRepository hotels;
    private final RoomTypeRepository roomTypes;
    private final RoomAvailabilityRepository availability;

    private final long seed;
    private final int daysAhead;

    public HotelDataGenerator(HotelRepository hotels,
                              RoomTypeRepository roomTypes,
                              RoomAvailabilityRepository availability,
                              @Value("${simulator.hotel.seed:67}") long seed,
                              @Value("${simulator.hotel.days-ahead:90}") int daysAhead) {
        this.hotels = hotels;
        this.roomTypes = roomTypes;
        this.availability = availability;
        this.seed = seed;
        this.daysAhead = daysAhead;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (hotels.count() > 0) {
            return;
        }
        Random random = new Random(seed);
        LocalDate firstNight = LocalDate.now(ZoneOffset.UTC).plusDays(1);

        for (City city : CITIES) {
            for (int i = 0; i < HOTELS_PER_CITY; i++) {
                generateHotel(city, random, firstNight);
            }
        }
    }

    private void generateHotel(City city, Random random, LocalDate firstNight) {
        int stars = 3 + random.nextInt(3);
        Hotel hotel = hotels.save(new Hotel(
                NAME_PREFIXES[random.nextInt(NAME_PREFIXES.length)] + " "
                        + city.name() + " "
                        + NAME_SUFFIXES[random.nextInt(NAME_SUFFIXES.length)],
                city.name(),
                "st. " + STREETS[random.nextInt(STREETS.length)] + ", " + (1 + random.nextInt(120)),
                stars,
                true,
                city.lat() + (random.nextDouble() - 0.5) * 0.05,
                city.lon() + (random.nextDouble() - 0.5) * 0.05));

        long basePrice = basePricePerNight(stars);

        for (TypeTemplate template : TYPES) {
            long typeBasePrice = roundToHundredRubles(Math.round(basePrice * template.priceFactor()));
            long roomsTotal = template.minRooms()
                    + random.nextInt(template.maxRooms() - template.minRooms() + 1);

            RoomType roomType = roomTypes.save(new RoomType(
                    template.name(), template.code(), template.capacity(),
                    typeBasePrice, roomsTotal, hotel));

            availability.saveAll(buildNights(roomType, (int) roomsTotal, typeBasePrice, random, firstNight));
        }
    }

    private long basePricePerNight(int stars) {
        long rubles_kop = switch (stars) {
            case 3 -> 350000;
            case 4 -> 600000;
            default -> 1100000;
        };
        return rubles_kop;
    }

    private long roundToHundredRubles(long kopecks) {
        long step = 10000;
        return Math.round((double) kopecks / step) * step;
    }

    private List<RoomAvailability> buildNights(RoomType roomType, int roomsTotal,
                                               long basePrice, Random random, LocalDate firstNight) {
        List<RoomAvailability> nights = new ArrayList<>(daysAhead);
        for (int day = 0; day < daysAhead; day++) {
            LocalDate date = firstNight.plusDays(day);

            int free = random.nextDouble() < SOLD_OUT_NIGHT_PROBABILITY
                    ? 0
                    : 1 + random.nextInt(roomsTotal);

            nights.add(new RoomAvailability(roomType, date, free, priceForNight(basePrice, date, random)));
        }
        return nights;
    }

    private long priceForNight(long basePrice, LocalDate date, Random random) {
        DayOfWeek dow = date.getDayOfWeek();
        double weekend = (dow == DayOfWeek.FRIDAY || dow == DayOfWeek.SATURDAY) ? 1.2 : 1.0;
        double jitter = 0.90 + random.nextDouble() * 0.20;
        return roundToHundredRubles(Math.round(basePrice * weekend * jitter));
    }
}