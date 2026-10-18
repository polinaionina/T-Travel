package ru.ttravel.orchestrator.domain.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

import ru.ttravel.orchestrator.domain.models.*;

@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_id")
    private Long orderId;

    /** Событие журнала, из-за которого создано уведомление. */
    @Column(name = "event_id")
    private Long eventId;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    protected Notification() {
    }

    public Notification(Long userId, Long orderId, Long eventId, String message) {
        this.userId = userId;
        this.orderId = orderId;
        this.eventId = eventId;
        this.message = message;
        this.status = NotificationStatus.UNREAD;
    }

    public void markRead() {
        this.status = NotificationStatus.READ;
        this.readAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getOrderId() { return orderId; }
    public Long getEventId() { return eventId; }
    public String getMessage() { return message; }
    public NotificationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
}