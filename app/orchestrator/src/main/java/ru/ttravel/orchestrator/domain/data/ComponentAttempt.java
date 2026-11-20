package ru.ttravel.orchestrator.domain.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

import ru.ttravel.orchestrator.domain.models.*;

/** Одна попытка обращения к поставщику по компоненту. */
@Entity
@Table(name = "component_attempt")
public class ComponentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "component_id", nullable = false)
    private Long componentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttemptOperation operation;

    /** Ключ идемпотентности вида {orderId}:{componentId}:{operation}. */
    @Column(name = "request_id")
    private String requestId;

    /** Пусто, пока попытка не завершилась. */
    @Enumerated(EnumType.STRING)
    private AttemptResult result;

    @Column(columnDefinition = "text")
    private String error;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected ComponentAttempt() {
    }

    public ComponentAttempt(Long componentId, AttemptOperation operation, String requestId) {
        this.componentId = componentId;
        this.operation = operation;
        this.requestId = requestId;
        this.startedAt = Instant.now();
    }

    /** Вызывается, когда попытка завершилась. */
    public void finish(AttemptResult result, String error) {
        this.result = result;
        this.error = error;
        this.finishedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getComponentId() { return componentId; }
    public AttemptOperation getOperation() { return operation; }
    public String getRequestId() { return requestId; }
    public AttemptResult getResult() { return result; }
    public String getError() { return error; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
}