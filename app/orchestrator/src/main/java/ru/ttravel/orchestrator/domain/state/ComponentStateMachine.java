package ru.ttravel.orchestrator.domain.state;

import org.springframework.stereotype.Component;
import ru.ttravel.orchestrator.domain.models.ComponentStatus;

import java.util.Map;
import java.util.Set;

import static ru.ttravel.orchestrator.domain.models.ComponentStatus.*;

@Component
public class ComponentStateMachine extends StateMachine<ComponentStatus> {

    public ComponentStateMachine() {
        super("Компонент", Map.of(
                PENDING, Set.of(RESERVING, CANCELLED),
                RESERVING, Set.of(RESERVED, FAILED, UNKNOWN),
                RESERVED, Set.of(CONFIRMED, UNKNOWN, COMPENSATING),
                CONFIRMED, Set.of(COMPENSATING),
                UNKNOWN, Set.of(
                        RESERVING,
                        RESERVED,
                        CONFIRMED,
                        FAILED,
                        COMPENSATING,
                        CANCELLED
                ),
                FAILED, Set.of(),
                COMPENSATING, Set.of(CANCELLED),
                CANCELLED, Set.of()
        ));
    }
}
