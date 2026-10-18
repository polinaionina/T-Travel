package ru.ttravel.orchestrator.domain.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/** Ключ идемпотентности: защита от создания второго заказа при повторном запросе. */
@Entity
@Table(name = "idempotency_key",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "key"}))
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "key", nullable = false)
    private String key;

    /** Хеш тела запроса. Если ключ тот же, а хеш другой, это конфликт (409). */
    @Column(name = "request_hash", nullable = false)
    private String requestHash;

    @Column(name = "order_id")
    private Long orderId;

    /** Ответ, который нужно вернуть при повторном запросе (JSON). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_snapshot")
    private String responseSnapshot;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IdempotencyKey() {
    }

    public IdempotencyKey(Long userId, String key, String requestHash) {
        this.userId = userId;
        this.key = key;
        this.requestHash = requestHash;
    }

    /** Запоминает созданный заказ и ответ для повторных запросов. */
    public void attachResult(Long orderId, String responseSnapshot) {
        this.orderId = orderId;
        this.responseSnapshot = responseSnapshot;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getKey() { return key; }
    public String getRequestHash() { return requestHash; }
    public Long getOrderId() { return orderId; }
    public String getResponseSnapshot() { return responseSnapshot; }
    public Instant getCreatedAt() { return createdAt; }
}