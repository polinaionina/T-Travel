package ru.ttravel.orchestrator.domain.data;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.ttravel.orchestrator.domain.models.*;

@Entity
@Table(name = "trip_order")
public class TripOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    /** Итоговая цена в копейках. */
    @Column(name = "total_price", nullable = false)
    private long totalPrice;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Оптимистичная блокировка. */
    @Version
    private long version;

    @OneToMany(mappedBy = "tripOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepNo ASC")
    private List<OrderComponent> components = new ArrayList<>();

    protected TripOrder() {
    }

    public TripOrder(Long userId, String currency) {
        this.userId = userId;
        this.currency = currency;
        this.status = OrderStatus.DRAFT;
        this.totalPrice = 0;
    }

    /** Добавляет компонент и увеличивает итоговую цену. */
    public void addComponent(OrderComponent component) {
        components.add(component);
        component.setTripOrder(this);
        totalPrice += component.getPrice();
    }

    /** Единственное место смены статуса. */
    public void changeStatus(OrderStatus newStatus) {
        this.status = newStatus;
    }

    public void markFailed(OrderStatus newStatus, String reason) {
        this.failureReason = reason;
        changeStatus(newStatus);
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public OrderStatus getStatus() { return status; }
    public long getTotalPrice() { return totalPrice; }
    public String getCurrency() { return currency; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
    public List<OrderComponent> getComponents() { return Collections.unmodifiableList(components); }
}