package ru.ttravel.orchestrator.domain.models;

import org.junit.jupiter.api.Test;
import ru.ttravel.orchestrator.domain.data.ComponentAttempt;
import static org.junit.jupiter.api.Assertions.*;

class ComponentAttemptTest {
    @Test 
    void newAttemptIsPending() {
        ComponentAttempt attempt = new ComponentAttempt(12L, AttemptOperation.CONFIRM, "req-1");
        assertEquals(12L, attempt.getComponentId());
        assertEquals(AttemptOperation.CONFIRM, attempt.getOperation());
        assertNull(attempt.getResult());
        assertNotNull(attempt.getStartedAt());
        assertNull(attempt.getFinishedAt());
    }

    @Test 
    void finishStoresResultErrorAndFinishTime() {
        ComponentAttempt attempt = new ComponentAttempt(12L, AttemptOperation.CONFIRM, "req-1");
        attempt.finish(AttemptResult.FAILED, "provider rejected");
        assertEquals(AttemptResult.FAILED, attempt.getResult());
        assertEquals("provider rejected", attempt.getError());
        assertNotNull(attempt.getFinishedAt());
    }
}
