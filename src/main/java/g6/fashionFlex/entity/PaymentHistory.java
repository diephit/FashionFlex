package g6.fashionFlex.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_history")
public class PaymentHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "historyID")
    private Integer historyID;

    @ManyToOne
    @JoinColumn(name = "paymentID", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "ENUM('pending','success','failed','refunded')")
    private PaymentStatus status;

    @Column(name = "changedAt")
    private LocalDateTime changedAt;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "gatewayResponse", columnDefinition = "JSON")
    private String gatewayResponse;

    @PrePersist
    protected void onCreate() {
        changedAt = LocalDateTime.now();
    }

    public PaymentHistory() {
    }

    public Integer getHistoryID() {
        return historyID;
    }

    public void setHistoryID(Integer historyID) {
        this.historyID = historyID;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getGatewayResponse() {
        return gatewayResponse;
    }

    public void setGatewayResponse(String gatewayResponse) {
        this.gatewayResponse = gatewayResponse;
    }

    public enum PaymentStatus {
        pending, success, failed, refunded
    }
}
