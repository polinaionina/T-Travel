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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

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

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

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

    public TripOrder(Long userId, String currency, String idempotencyKey, String requestHash) {
        this.userId = userId;
        this.currency = currency;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.status = OrderStatus.DRAFT;
        this.totalPrice = 0;
    }

    /** Добавляет компонент и увеличивает итоговую цену. */
    public void addComponent(OrderComponent component) {
        components.add(component);
        component.setTripOrder(this);
        totalPrice += component.getPrice();
    }

    /**
     * Возвращает актуальные компоненты: для каждого типа последний добавленный.
     * Ранее заменённые компоненты остаются в заказе только для истории.
     *
     * @return актуальные компоненты в порядке шагов
     */
    public List<OrderComponent> activeComponents() {
        Map<ComponentType, OrderComponent> latest = new EnumMap<>(ComponentType.class);
        for (OrderComponent component : components) {
            latest.put(component.getType(), component);
        }
        return latest.values().stream()
                .sorted(Comparator.comparingLong(OrderComponent::getStepNo))
                .toList();
    }

    /**
     * Пересчитывает итоговую цену по актуальным компонентам в статусах
     * {@code RESERVED} и {@code CONFIRMED}.
     */
    public void recalculateTotal() {
        this.totalPrice = activeComponents().stream()
                .filter(c -> c.getStatus() == ComponentStatus.RESERVED
                        || c.getStatus() == ComponentStatus.CONFIRMED)
                .mapToLong(OrderComponent::getPrice)
                .sum();
    }

    /**
     * Отмечает, что с заказом работали, чтобы он не считался заброшенным.
     */
    public void touch() {
        this.updatedAt = Instant.now();
    }

    /** Единственное место смены статуса. */
    public void changeStatus(OrderStatus newStatus) {
        this.status = newStatus;
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
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestHash() { return requestHash; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
    public List<OrderComponent> getComponents() { return Collections.unmodifiableList(components); }
}