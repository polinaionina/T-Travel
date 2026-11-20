package ru.ttravel.orchestrator.domain.state;

import org.springframework.stereotype.Component;
import ru.ttravel.orchestrator.domain.models.OrderStatus;

import java.util.Map;
import java.util.Set;

import static ru.ttravel.orchestrator.domain.models.OrderStatus.*;

/**
 * Допустимые переходы статуса заказа.
 */
@Component
public class OrderStateMachine extends StateMachine<OrderStatus> {

    public OrderStateMachine() {
        super("Заказ", Map.of(
                DRAFT, Set.of(SUBMITTED, CANCELLED),
                SUBMITTED, Set.of(CONFIRMING, CANCELLED),
                CONFIRMING, Set.of(CONFIRMED, COMPENSATING, ACTION_REQUIRED),
                ACTION_REQUIRED, Set.of(COMPENSATING, SUBMITTED),
                CONFIRMED, Set.of(),
                COMPENSATING, Set.of(CANCELLED, COMPENSATION_FAILED),
                CANCELLED, Set.of(),
                COMPENSATION_FAILED, Set.of(COMPENSATING)
        ));
    }
}