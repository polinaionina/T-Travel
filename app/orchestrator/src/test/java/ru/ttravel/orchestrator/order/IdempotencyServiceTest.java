package ru.ttravel.orchestrator.order;

import org.junit.jupiter.api.Test;

import ru.ttravel.orchestrator.domain.data.TripOrder;
import ru.ttravel.orchestrator.domain.exceptions.IdempotencyConflictException;
import ru.ttravel.orchestrator.domain.repositories.OrderRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IdempotencyServiceTest {

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final IdempotencyService service =
            new IdempotencyService(orderRepository);

    @Test
    void calculateHashReturnsSha256Hash() {
        String hash = service.calculateHash("test request");

        assertNotNull(hash);
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]{64}"));
    }

    @Test
    void calculateHashReturnsSameValueForSameInput() {
        String first = service.calculateHash("same request");
        String second = service.calculateHash("same request");

        assertEquals(first, second);
    }

    @Test
    void calculateHashReturnsDifferentValuesForDifferentInputs() {
        String first = service.calculateHash("request one");
        String second = service.calculateHash("request two");

        assertNotEquals(first, second);
    }

    @Test
    void checkIdempotencyReturnsEmptyWhenOrderDoesNotExist() throws Exception {
        when(orderRepository.findByUserIdAndIdempotencyKey(10L, "key"))
                .thenReturn(Optional.empty());

        Optional<TripOrder> result =
                service.checkIdempotency(10L, "key", "hash");

        assertTrue(result.isEmpty());
    }

    @Test
    void checkIdempotencyReturnsExistingOrderForSameRequest() throws Exception {
        TripOrder order = new TripOrder(10L, "RUB", "key", "hash");

        when(orderRepository.findByUserIdAndIdempotencyKey(10L, "key"))
                .thenReturn(Optional.of(order));

        Optional<TripOrder> result =
                service.checkIdempotency(10L, "key", "hash");

        assertTrue(result.isPresent());
        assertSame(order, result.get());
    }

    @Test
    void checkIdempotencyThrowsWhenSameKeyHasDifferentRequest()
            throws Exception {
        TripOrder order = new TripOrder(
                10L, "RUB", "key", "original-hash"
        );

        when(orderRepository.findByUserIdAndIdempotencyKey(10L, "key"))
                .thenReturn(Optional.of(order));

        assertThrows(
                IdempotencyConflictException.class,
                () -> service.checkIdempotency(
                        10L, "key", "different-hash"
                )
        );
    }

    @Test
    void findExistingOrderUsesUserAndIdempotencyKey() {
        TripOrder order = new TripOrder(10L, "RUB", "key", "hash");

        when(orderRepository.findByUserIdAndIdempotencyKey(10L, "key"))
                .thenReturn(Optional.of(order));

        Optional<TripOrder> result =
                service.findExistingOrder(10L, "key");

        assertSame(order, result.orElseThrow());
        verify(orderRepository).findByUserIdAndIdempotencyKey(10L, "key");
    }
}
