package ru.ttravel.suppliers.airline.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
import ru.ttravel.suppliers.airline.service.AirlineService;

@RestController
@RequestMapping("/airline")
public class AirlineController {

    private final AirlineService service;

    public AirlineController(AirlineService service) {
        this.service = service;
    }

    @PostMapping("/reserve")
    public ResponseEntity<ReserveResponse> reserve(@RequestBody ReserveRequest request) {
        ReserveResponse response = service.reserve(request);
        return ResponseEntity.status(httpStatus(response.status(), response.error())).body(response);
    }

    @PostMapping("/confirm")
    public ResponseEntity<ConfirmResponse> confirm(@RequestBody ConfirmRequest request) {
        ConfirmResponse response = service.confirm(request);
        return ResponseEntity.status(httpStatus(response.status(), response.error())).body(response);
    }

    @PostMapping("/cancel")
    public ResponseEntity<CancelResponse> cancel(@RequestBody CancelRequest request) {
        CancelResponse response = service.cancel(request);
        return ResponseEntity.status(httpStatus(response.status(), response.error())).body(response);
    }

    @PostMapping("/status")
    public BookingStatusResponse status(@RequestBody BookingStatusRequest request) {
        return service.status(request);
    }

    private HttpStatus httpStatus(SupplierOperationStatus status, SupplierError error) {
        if (status == SupplierOperationStatus.SUCCESS) {
            return HttpStatus.OK;
        }
        if (error == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return switch (error.code()) {
            case "INVALID_REQUEST" -> HttpStatus.BAD_REQUEST;
            case "BOOKING_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "OFFER_UNAVAILABLE",
                 "INVALID_STATE" -> HttpStatus.CONFLICT;
            case "SUPPLIER_UNAVAILABLE" -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}