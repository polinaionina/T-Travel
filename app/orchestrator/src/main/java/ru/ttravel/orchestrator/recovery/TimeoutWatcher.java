package ru.ttravel.orchestrator.recovery;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ru.ttravel.orchestrator.domain.data.OrderComponent;
import ru.ttravel.orchestrator.domain.models.ComponentStatus;
import ru.ttravel.orchestrator.domain.repositories.OrderComponentRepository;

import java.time.Instant;
import java.util.List;
import java.util.Set;

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
            return;
        }

        handleOverdueComponents(overdueComponents);
    }

    /**
     * Обрабатывает список обнаруженных просроченных компонентов.
     *
     * @param components список просроченных компонентов
     */
    private void handleOverdueComponents(List<OrderComponent> components) {
        for (OrderComponent component : components) {
            System.out.println(
                    "Обнаружен просроченный компонент: " + component.getId()
            );
        }
    }
}