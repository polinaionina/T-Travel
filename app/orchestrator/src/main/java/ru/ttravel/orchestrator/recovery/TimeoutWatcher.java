package ru.ttravel.orchestrator.recovery;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ru.ttravel.orchestrator.domain.data.OrderComponent;
import ru.ttravel.orchestrator.domain.models.ComponentStatus;
import ru.ttravel.orchestrator.domain.repositories.OrderComponentRepository;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Периодически проверяет компоненты заказов на истечение дедлайнов.
 */
@Component
public class TimeoutWatcher {

    private static final Set<ComponentStatus> WATCHED_STATUSES = Set.of(
            ComponentStatus.RESERVING,
            ComponentStatus.RESERVED,
            ComponentStatus.UNKNOWN
    );

    private final OrderComponentRepository componentRepository;

    private static final Logger log = LoggerFactory.getLogger(TimeoutWatcher.class);

    public TimeoutWatcher(OrderComponentRepository componentRepository) {
        this.componentRepository = componentRepository;
    }

    /**
     * Проверяет наличие просроченных компонентов.
     *
     * Метод запускается автоматически каждую минуту.
     */
    @Scheduled(fixedDelay = 60_000)
    public void checkTimeouts() {
        Instant now = Instant.now();

        List<OrderComponent> overdueComponents =
                componentRepository.findByStatusInAndDeadlineAtBefore(
                        WATCHED_STATUSES,
                        now
                );

        if (overdueComponents.isEmpty()) {
            log.debug("Timeout check completed: no overdue components");
            return;
        }

        log.warn("Found {} overdue components",
            overdueComponents.size()
        );

        handleOverdueComponents(overdueComponents);
    }

    /**
     * Обрабатывает список обнаруженных просроченных компонентов.
     *
     * @param components список просроченных компонентов
     */
    private void handleOverdueComponents(List<OrderComponent> components) {
        for (OrderComponent component : components) {
            log.warn("Overdue component detected: componentId={}, orderId={}, status={}, deadlineAt={}",
                    component.getId(),
                    component.getTripOrder().getId(),
                    component.getStatus(),
                    component.getDeadlineAt()
            );
        }
    }
}