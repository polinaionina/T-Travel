package ru.ttravel.suppliers.insurance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * An issued insurance policy: the "booking" of the insurance simulator.
 * <p>
 * A repeated request with the same {@code requestId} does not issue a new
 * policy but returns the existing one, which is why {@code requestId} is unique.
 * <p>
 * The status, cancellation time and refund are changed only together,
 * via {@link #cancel(Long)}.
 */
@Entity
@Table(name = "policy")
public class Policy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", referencedColumnName = "id", nullable = false)
    private InsuranceProgram program;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "request_id", nullable = false, unique = true)
    private String requestId;

    /** Policy state: {@code CONFIRMED} or {@code CANCELLED}. */
    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "total_price", nullable = false)
    private Long totalPrice;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "refund_amount", nullable = true)
    private Long refundAmount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "cancelled_at", nullable = true)
    private OffsetDateTime cancelledAt;

    /** Empty constructor, required by Hibernate. */
    protected Policy() {
    }

    /**
     * Creates a new policy and records the creation time.
     *
     * @param program    insurance program
     * @param startDate  first day of coverage
     * @param endDate    last day of coverage, not before {@code startDate}
     * @param requestId  idempotency key from the orchestrator
     * @param status     initial status, usually {@code CONFIRMED}
     * @param totalPrice total price in kopecks
     * @param currency   ISO 4217 currency code
     */
    public Policy(InsuranceProgram program, LocalDate startDate, LocalDate endDate,
                  String requestId, String status, Long totalPrice, String currency) {
        this.program = program;
        this.startDate = startDate;
        this.endDate = endDate;
        this.requestId = requestId;
        this.status = status;
        this.totalPrice = totalPrice;
        this.currency = currency;
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public InsuranceProgram getProgram() { return program; }
    public void setProgram(InsuranceProgram program) { this.program = program; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getStatus() { return status; }

    public Long getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Long totalPrice) { this.totalPrice = totalPrice; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Long getRefundAmount() { return refundAmount; }

    public OffsetDateTime getCreatedAt() { return createdAt; }

    public OffsetDateTime getCancelledAt() { return cancelledAt; }

    /**
     * Cancels the policy: sets the status to {@code CANCELLED}, records the
     * cancellation time and the refund.
     * <p>
     * The operation is idempotent: if the policy is already cancelled, the method
     * changes nothing and keeps the first cancellation time and refund.
     * <p>
     * The method does not check whether coverage has already started: the
     * cancellation service must reject such requests with {@code CANNOT_CANCEL}
     * before calling this method.
     *
     * @param refundAmount amount to return, in kopecks
     */
    public void cancel(Long refundAmount) {
        if ("CANCELLED".equals(status)) {
            return;
        }
        this.status = "CANCELLED";
        this.cancelledAt = OffsetDateTime.now(ZoneOffset.UTC);
        this.refundAmount = refundAmount;
    }
}