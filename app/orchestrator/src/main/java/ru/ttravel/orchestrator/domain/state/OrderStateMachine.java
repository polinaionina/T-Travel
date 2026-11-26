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
                DRAFT, Set.of(CONFIRMING, CANCELLED),
                CONFIRMING, Set.of(PAYMENT_PENDING, ACTION_REQUIRED),
                ACTION_REQUIRED, Set.of(CONFIRMING, CANCELLING),
                PAYMENT_PENDING, Set.of(CONFIRMED, PAYMENT_FAILED),
                CONFIRMED, Set.of(),
                PAYMENT_FAILED, Set.of(CANCELLED),
                CANCELLING, Set.of(CANCELLED, NEEDS_HELP),
                CANCELLED, Set.of(),
                NEEDS_HELP, Set.of(CANCELLING)
        ));
    }
}