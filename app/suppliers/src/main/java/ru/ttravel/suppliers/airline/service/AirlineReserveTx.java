package ru.ttravel.suppliers.airline.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ru.ttravel.contracts.supplier.models.SupplierError;
import ru.ttravel.contracts.supplier.models.SupplierOperationStatus;
import ru.ttravel.contracts.supplier.request.ReserveRequest;
import ru.ttravel.contracts.supplier.response.ReserveResponse;
import ru.ttravel.suppliers.airline.domain.Booking;
import ru.ttravel.suppliers.airline.domain.BookingStatus;
import ru.ttravel.suppliers.airline.domain.Flight;
import ru.ttravel.suppliers.airline.domain.FlightFare;
import ru.ttravel.suppliers.airline.repository.BookingRepository;
import ru.ttravel.suppliers.airline.repository.FlightFareRepository;

@Component
public class AirlineReserveTx {

    private static final String CURRENCY = "RUB";

    private final FlightFareRepository fares;
    private final BookingRepository bookings;

    public AirlineReserveTx(FlightFareRepository fares, BookingRepository bookings) {
        this.fares = fares;
        this.bookings = bookings;
    }

    @Transactional
    public ReserveResponse reserve(ReserveRequest request) {
        if (request == null || request.requestId() == null || request.requestId().isBlank()
                || request.offerId() == null) {
            return failed("INVALID_REQUEST", "requestId and offerId are required");
        }

        Booking existing = bookings.findByRequestId(request.requestId()).orElse(null);
        if (existing != null) {
            return replayOf(existing);
        }

        Long fareId = parseId(request.offerId());
        if (fareId == null) {
            return failed("INVALID_REQUEST", "Invalid offerId: " + request.offerId());
        }

        FlightFare fare = fares.findById(fareId).orElse(null);
        if (fare == null) {
            return failed("OFFER_UNAVAILABLE", "Offer not found: " + fareId);
        }

        Flight flight = fare.getFlight();
        if (!"SCHEDULED".equals(flight.getStatus())
                || flight.getScheduledDeparture().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            return failed("OFFER_UNAVAILABLE", "Flight is not on sale");
        }

        if (fares.takeSeats(fareId, 1) == 0) {
            return failed("OFFER_UNAVAILABLE", "No free seats");
        }

        FlightFare managedFare = fares.getReferenceById(fareId);
        Booking booking = bookings.saveAndFlush(new Booking(
                managedFare,
                String.valueOf(fareId),
                request.requestId(),
                BookingStatus.RESERVED,
                managedFare.getPrice()));

        return new ReserveResponse(
                SupplierOperationStatus.SUCCESS,
                String.valueOf(booking.getId()),
                booking.getPrice(),
                CURRENCY,
                null);
    }

    @Transactional(readOnly = true)
    public Optional<ReserveResponse> replay(String requestId) {
        return bookings.findByRequestId(requestId).map(this::replayOf);
    }

    private ReserveResponse replayOf(Booking booking) {
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return failed("INVALID_STATE", "Booking for this requestId was already cancelled");
        }
        return new ReserveResponse(
                SupplierOperationStatus.SUCCESS,
                String.valueOf(booking.getId()),
                booking.getPrice(),
                CURRENCY,
                null);
    }

    private ReserveResponse failed(String code, String message) {
        return new ReserveResponse(SupplierOperationStatus.FAILED, null, null, null,
                new SupplierError(code, message));
    }

    private Long parseId(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}