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
import ru.ttravel.orchestrator.domain.models.ComponentStatus;
import ru.ttravel.orchestrator.domain.models.ComponentType;

import java.time.Instant;

/**
 * Компонент заказа: перелёт, гостиница, страховка или трансфер.
 */
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

    /** Номер шага (страницы), берётся из {@link ComponentType#getStepNo()}. */
    @Column(name = "step_no", nullable = false)
    private long stepNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComponentStatus status;

    @Column(nullable = false)
    private long price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "provider_offer_id")
    private String providerOfferId;

    /** До какого момента нужно подтвердить компонент (таймаут). */
    @Column(name = "deadline_at")
    private Instant deadlineAt;

    /** Копия предложения на момент выбора (JSON). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "offer_snapshot")
    private String offerSnapshot;

    /** Идентификатор брони у поставщика, появляется после {@code reserve}. */
    @Column(name = "external_booking_id")
    private String externalBookingId;

    protected OrderComponent() {
    }

    /**
     * Создаёт компонент в статусе {@code PENDING}.
     *
     * @param type тип компонента
     * @param price цена в копейках
     * @param currency код валюты
     * @param providerOfferId идентификатор предложения у поставщика
     * @param offerSnapshot копия предложения в формате JSON
     */
    public OrderComponent(ComponentType type, long price, String currency,
                          String providerOfferId, String offerSnapshot) {
        this.type = type;
        this.stepNo = type.getStepNo();
        this.price = price;
        this.currency = currency;
        this.providerOfferId = providerOfferId;
        this.offerSnapshot = offerSnapshot;
        this.status = ComponentStatus.PENDING;
    }

    /**
     * Меняет статус компонента.
     * @param newStatus новый статус
     */
    public void changeStatus(ComponentStatus newStatus) {
        this.status = newStatus;
    }

    void setTripOrder(TripOrder tripOrder) { this.tripOrder = tripOrder; }

    public void setExternalBookingId(String externalBookingId) { this.externalBookingId = externalBookingId; }
    public void setDeadlineAt(Instant deadlineAt) { this.deadlineAt = deadlineAt; }
    public void setPrice(long price) { this.price = price; }

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