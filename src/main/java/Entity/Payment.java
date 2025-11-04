package Entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "paymentID")
    private Integer paymentID;

    @ManyToOne
    @JoinColumn(name = "orderID", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, columnDefinition = "ENUM('VNPay','CreditCard','Momo')")
    private PaymentMethod method;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "ENUM('pending','success','failed','refunded') DEFAULT 'pending'")
    private PaymentStatus status;

    @Column(name = "transactionCode", length = 100)
    private String transactionCode;

    @Column(name = "paymentDate")
    private LocalDateTime paymentDate;

    @Column(name = "paymentDetails", columnDefinition = "JSON")
    private String paymentDetails;

    @Column(name = "createdAt")
    private LocalDateTime createdAt;

    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL)
    private List<PaymentHistory> paymentHistories;

    @PrePersist
    protected void onCreate() {
        paymentDate = LocalDateTime.now();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = PaymentStatus.pending;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Payment() {
    }

    public Integer getPaymentID() {
        return paymentID;
    }

    public void setPaymentID(Integer paymentID) {
        this.paymentID = paymentID;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getTransactionCode() {
        return transactionCode;
    }

    public void setTransactionCode(String transactionCode) {
        this.transactionCode = transactionCode;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getPaymentDetails() {
        return paymentDetails;
    }

    public void setPaymentDetails(String paymentDetails) {
        this.paymentDetails = paymentDetails;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<PaymentHistory> getPaymentHistories() {
        return paymentHistories;
    }

    public void setPaymentHistories(List<PaymentHistory> paymentHistories) {
        this.paymentHistories = paymentHistories;
    }

    public enum PaymentMethod {
        VNPay, CreditCard, Momo
    }

    public enum PaymentStatus {
        pending, success, failed, refunded
    }
}

