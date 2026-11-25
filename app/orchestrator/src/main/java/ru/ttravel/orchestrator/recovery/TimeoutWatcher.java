package ru.ttravel.orchestrator.recovery;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.ttravel.orchestrator.booking.CompensationService;
import ru.ttravel.orchestrator.domain.data.OrderComponent;
import ru.ttravel.orchestrator.domain.data.TripOrder;
import ru.ttravel.orchestrator.domain.exceptions.OrchestratorException;
import ru.ttravel.orchestrator.domain.models.ComponentStatus;
import ru.ttravel.orchestrator.domain.models.OrderStatus;
import ru.ttravel.orchestrator.domain.repositories.OrderComponentRepository;
import ru.ttravel.orchestrator.domain.repositories.OrderRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Component
public class TimeoutWatcher {

    private static final Logger log =
            LoggerFactory.getLogger(TimeoutWatcher.class);

    private static final Duration USER_ACTION_TIMEOUT =
            Duration.ofMinutes(30);

    private static final Set<ComponentStatus> WATCHED_STATUSES = Set.of(
            ComponentStatus.RESERVING,
            ComponentStatus.RESERVED,
            ComponentStatus.UNKNOWN
    );

    private final OrderComponentRepository componentRepository;
    private final OrderRepository orderRepository;
    private final CompensationService compensationService;

    public TimeoutWatcher(
            OrderComponentRepository componentRepository,
            OrderRepository orderRepository,
            CompensationService compensationService
    ) {
        this.componentRepository = componentRepository;
        this.orderRepository = orderRepository;
        this.compensationService = compensationService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void checkTimeouts() {
        Instant now = Instant.now();

        checkHoldTimeouts(now);
        checkUserActionTimeouts(now);
    }

    private void checkHoldTimeouts(Instant now) {
        List<OrderComponent> overdueComponents =
                componentRepository.findByStatusInAndDeadlineAtBefore(
                        WATCHED_STATUSES,
                        now
                );

        for (OrderComponent component : overdueComponents) {
            TripOrder order = component.getTripOrder();

            if (order.getStatus() != OrderStatus.DRAFT
                    && order.getStatus() != OrderStatus.CONFIRMING
                    && order.getStatus() != OrderStatus.PAYMENT_FAILED) {
                continue;
            }

            if (component.getStatus() == ComponentStatus.UNKNOWN
                    || component.getStatus() == ComponentStatus.RESERVING) {
                log.warn(
                        "Overdue component requires status resolution: orderId={}, componentId={}, status={}",
                        order.getId(),
                        component.getId(),
                        component.getStatus()
                );
                continue;
            }

            log.warn(
                    "Hold timeout: starting compensation, orderId={}, componentId={}",
                    order.getId(),
                    component.getId()
            );

            startCompensation(order);
        }
    }

    private void checkUserActionTimeouts(Instant now) {
        Instant deadline = now.minus(USER_ACTION_TIMEOUT);

        List<TripOrder> overdueOrders =
                orderRepository.findByStatusAndUpdatedAtBefore(
                        OrderStatus.ACTION_REQUIRED,
                        deadline
                );

        for (TripOrder order : overdueOrders) {
            log.warn(
                    "User action timeout: starting compensation, orderId={}",
                    order.getId()
            );

            startCompensation(order);
        }
    }

    private void startCompensation(TripOrder order) {
        try {
            compensationService.startCompensation(
                    order.getId(),
                    order.getUserId()
            );
        } catch (OrchestratorException exception) {
            log.error(
                    "Failed to start compensation: orderId={}",
                    order.getId(),
                    exception
            );
        }
    }
}