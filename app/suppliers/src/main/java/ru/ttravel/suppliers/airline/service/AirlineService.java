package ru.ttravel.suppliers.airline.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.ttravel.contracts.supplier.models.SupplierBookingStatus;
import ru.ttravel.contracts.supplier.models.SupplierError;
import ru.ttravel.contracts.supplier.models.SupplierOperationStatus;
import ru.ttravel.contracts.supplier.request.BookingStatusRequest;
import ru.ttravel.contracts.supplier.request.CancelRequest;
import ru.ttravel.contracts.supplier.request.ConfirmRequest;
import ru.ttravel.contracts.supplier.request.ReserveRequest;
import ru.ttravel.contracts.supplier.response.BookingStatusResponse;
import ru.ttravel.contracts.supplier.response.CancelResponse;
import ru.ttravel.contracts.supplier.response.ConfirmResponse;
import ru.ttravel.contracts.supplier.response.ReserveResponse;
import ru.ttravel.suppliers.airline.domain.Booking;
import ru.ttravel.suppliers.airline.domain.BookingStatus;
import ru.ttravel.suppliers.airline.domain.Flight;
import ru.ttravel.suppliers.airline.domain.FlightFare;
import ru.ttravel.suppliers.airline.repository.BookingRepository;
import ru.ttravel.suppliers.airline.repository.FlightFareRepository;

@Service
public class AirlineService {

    private static final String CURRENCY = "RUB";

    private final FlightFareRepository fares;
    private final BookingRepository bookings;
    private final AirlineReserveTx reserveTx;

    public AirlineService(FlightFareRepository fares,
                          BookingRepository bookings,
                          AirlineReserveTx reserveTx) {
        this.fares = fares;
        this.bookings = bookings;
        this.reserveTx = reserveTx;
    }

    // ---------- offers (OffersRequest / OfferResponse are provisional) ----------

    // @Transactional(readOnly = true)
    // public List<OfferResponse> offers(OffersRequest request) {
    //     OffsetDateTime from = request.dateFrom().atStartOfDay().atOffset(ZoneOffset.UTC);
    //     OffsetDateTime to = request.dateTo().plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
    //     OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    //     if (from.isBefore(now)) {
    //         from = now;
    //     }

    //     List<OfferResponse> result = new ArrayList<>();
    //     for (FlightFare fare : fares.search(request.fromCityId(), request.toCityId(), from, to)) {
    //         result.add(toOffer(fare));
    //     }
    //     return result;
    // }

    // ---------- reserve (no transaction here on purpose) ----------

    public ReserveResponse reserve(ReserveRequest request) {
        try {
            return reserveTx.reserve(request);
        } catch (DataIntegrityViolationException e) {
            // two identical requests raced and the other one won: return its booking
            return reserveTx.replay(request.requestId()).orElseThrow(() -> e);
        }
    }

    // ---------- confirm ----------

    @Transactional
    public ConfirmResponse confirm(ConfirmRequest request) {
        Booking booking = find(request.externalBookingId());
        if (booking == null) {
            return confirmFailed(request.externalBookingId(),
                    "BOOKING_NOT_FOUND",
                    "Booking not found: " + request.externalBookingId());
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return confirmFailed(request.externalBookingId(),
                    "INVALID_STATE",
                    "Booking is cancelled and cannot be confirmed");
        }

        booking.confirm(); // repeated confirmation is a no-op
        return new ConfirmResponse(SupplierOperationStatus.SUCCESS,
                String.valueOf(booking.getId()), null);
    }

    // ---------- cancel ----------

    @Transactional
    public CancelResponse cancel(CancelRequest request) {
        Booking booking = find(request.externalBookingId());
        if (booking == null) {
            return new CancelResponse(SupplierOperationStatus.FAILED,
                    new SupplierError("BOOKING_NOT_FOUND",
                            "Booking not found: " + request.externalBookingId()));
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return new CancelResponse(SupplierOperationStatus.SUCCESS, null); // seat is not released twice
        }

        Long fareId = booking.getFare().getId();
        booking.cancel();
        bookings.saveAndFlush(booking);
        fares.releaseSeats(fareId, 1);

        return new CancelResponse(SupplierOperationStatus.SUCCESS, null);
    }

    // ---------- status ----------

    @Transactional(readOnly = true)
    public BookingStatusResponse status(BookingStatusRequest request) {
        Booking booking = find(request.externalBookingId());
        if (booking == null) {
            return new BookingStatusResponse(SupplierBookingStatus.NOT_FOUND,
                    request.externalBookingId());
        }
        return new BookingStatusResponse(
                SupplierBookingStatus.valueOf(booking.getStatus().name()),
                String.valueOf(booking.getId()));
    }

    // ---------- helpers ----------

    private Booking find(String externalBookingId) {
        if (externalBookingId == null) {
            return null;
        }
        try {
            return bookings.findById(Long.valueOf(externalBookingId)).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ConfirmResponse confirmFailed(String externalBookingId, String code, String message) {
        return new ConfirmResponse(SupplierOperationStatus.FAILED, externalBookingId,
                new SupplierError(code, message));
    }

    // private OfferResponse toOffer(FlightFare fare) {
    //     Flight f = fare.getFlight();
    //     Map<String, Object> details = new LinkedHashMap<>();
    //     details.put("flightNo", f.getFlightNo());
    //     details.put("fromAirport", f.getDepartureAirport().getCode());
    //     details.put("toAirport", f.getArrivalAirport().getCode());
    //     details.put("fromCityId", f.getDepartureAirport().getCityId());
    //     details.put("toCityId", f.getArrivalAirport().getCityId());
    //     details.put("departure", f.getScheduledDeparture().toInstant().toString());
    //     details.put("arrival", f.getScheduledArrival().toInstant().toString());
    //     details.put("fareClass", fare.getFareClass());
    //     details.put("seatsAvailable", fare.getSeatsAvailable());
    //     return new OfferResponse(String.valueOf(fare.getId()), "FLIGHT",
    //             fare.getPrice(), CURRENCY, details);
    // }
}