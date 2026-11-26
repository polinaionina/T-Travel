package ru.ttravel.orchestrator.recovery;

import org.junit.jupiter.api.Test;

import ru.ttravel.orchestrator.domain.data.TripOrder;
import ru.ttravel.orchestrator.domain.models.OrderStatus;
import ru.ttravel.orchestrator.domain.repositories.OrderRepository;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

class RecoveryServiceTest {

    private final OrderRepository repository = mock(OrderRepository.class);

    private final RecoveryService service = new RecoveryService(repository);

    @Test
    void findOrdersForRecoverySearchesAllRecoverableStatuses() {
        List<TripOrder> orders = List.of(
                new TripOrder(1L, "RUB", "key-1", "hash-1")
        );

        when(repository.findByStatusIn(anyCollection()))
                .thenReturn(orders);

        List<TripOrder> result = service.findOrdersForRecovery();

        assertSame(orders, result);

        verify(repository).findByStatusIn(argThat(statuses ->
                statuses.containsAll(Set.of(
                        OrderStatus.CONFIRMING,
                        OrderStatus.PAYMENT_PENDING,
                        OrderStatus.CANCELLING
                )) && statuses.size() == 3
        ));
    }

    @Test
    void findOrdersForConfirmationRecoverySearchesOnlyConfirming() {
        when(repository.findByStatusIn(Set.of(OrderStatus.CONFIRMING)))
                .thenReturn(List.of());

        List<TripOrder> result =
                service.findOrdersForConfirmationRecovery();

        assertTrue(result.isEmpty());
        verify(repository).findByStatusIn(Set.of(OrderStatus.CONFIRMING));
    }

    @Test
    void findOrdersForCompensationRecoverySearchesOnlyCancelling() {
        when(repository.findByStatusIn(Set.of(OrderStatus.CANCELLING)))
                .thenReturn(List.of());

        List<TripOrder> result =
                service.findOrdersForCompensationRecovery();

        assertTrue(result.isEmpty());
        verify(repository).findByStatusIn(Set.of(OrderStatus.CANCELLING));
    }
}
