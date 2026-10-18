package ru.ttravel.orchestrator.domain.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

import ru.ttravel.orchestrator.domain.models.*;

@Entity
@Table(name = "order_component")
public class OrderComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private TripOrder tripOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComponentType type;

    /** Порядок шагов: одинаковый номер значит параллельно, больший номер идёт позже. */
    @Column(name = "step_no", nullable = false)
    private long stepNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComponentStatus status;

    /** Цена в копейках. */
    @Column(nullable = false)
    private long price;

    @Column(nullable = false, length = 3)
    private String currency;

    /** Идентификатор предложения у поставщика. */
    @Column(name = "provider_offer_id")
    private String providerOfferId;

    /** До какого момента нужно подтвердить компонент (таймаут). */
    @Column(name = "deadline_at")
    private Instant deadlineAt;

    /** Копия предложения на момент выбора (JSON). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "offer_snapshot")
    private String offerSnapshot;

    /** Идентификатор брони у поставщика, появляется после reserve. */
    @Column(name = "external_booking_id")
    private String externalBookingId;

    protected OrderComponent() {
    }

    public OrderComponent(ComponentType type, long stepNo, long price, String currency,
                          String providerOfferId, String offerSnapshot) {
        this.type = type;
        this.stepNo = stepNo;
        this.price = price;
        this.currency = currency;
        this.providerOfferId = providerOfferId;
        this.offerSnapshot = offerSnapshot;
        this.status = ComponentStatus.PENDING;
    }

    /** Единственное место смены статуса компонента. */
    public void changeStatus(ComponentStatus newStatus) {
        this.status = newStatus;
    }

    public void setExternalBookingId(String externalBookingId) { this.externalBookingId = externalBookingId; }
    public void setDeadlineAt(Instant deadlineAt) { this.deadlineAt = deadlineAt; }
    public void setPrice(long price) { this.price = price; }

    /** Вызывается из TripOrder.addComponent. */
    void setTripOrder(TripOrder tripOrder) { this.tripOrder = tripOrder; }

    public Long getId() { return id; }
    public TripOrder getTripOrder() { return tripOrder; }
    public ComponentType getType() { return type; }
    public long getStepNo() { return stepNo; }
    public ComponentStatus getStatus() { return status; }
    public long getPrice() { return price; }
    public String getCurrency() { return currency; }
    public String getProviderOfferId() { return providerOfferId; }
    public Instant getDeadlineAt() { return deadlineAt; }
    public String getOfferSnapshot() { return offerSnapshot; }
    public String getExternalBookingId() { return externalBookingId; }
}