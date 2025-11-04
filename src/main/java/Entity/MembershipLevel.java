package Entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "membership_levels")
public class MembershipLevel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "levelID")
    private Integer levelID;

    @Column(name = "levelName", nullable = false, length = 50)
    private String levelName;

    @Column(name = "minSpent", nullable = false, precision = 12, scale = 2)
    private BigDecimal minSpent;

    @Column(name = "discountRate", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountRate;

    @Column(name = "bonusRate", nullable = false, precision = 5, scale = 2)
    private BigDecimal bonusRate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "createdAt")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "level")
    private List<Customer> customers;

    @OneToMany(mappedBy = "newLevel")
    private List<MembershipHistory> newLevelHistories;

    @OneToMany(mappedBy = "oldLevel")
    private List<MembershipHistory> oldLevelHistories;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public MembershipLevel() {
    }

    public Integer getLevelID() {
        return levelID;
    }

    public void setLevelID(Integer levelID) {
        this.levelID = levelID;
    }

    public String getLevelName() {
        return levelName;
    }

    public void setLevelName(String levelName) {
        this.levelName = levelName;
    }

    public BigDecimal getMinSpent() {
        return minSpent;
    }

    public void setMinSpent(BigDecimal minSpent) {
        this.minSpent = minSpent;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(BigDecimal discountRate) {
        this.discountRate = discountRate;
    }

    public BigDecimal getBonusRate() {
        return bonusRate;
    }

    public void setBonusRate(BigDecimal bonusRate) {
        this.bonusRate = bonusRate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Customer> getCustomers() {
        return customers;
    }

    public void setCustomers(List<Customer> customers) {
        this.customers = customers;
    }

    public List<MembershipHistory> getNewLevelHistories() {
        return newLevelHistories;
    }

    public void setNewLevelHistories(List<MembershipHistory> newLevelHistories) {
        this.newLevelHistories = newLevelHistories;
    }

    public List<MembershipHistory> getOldLevelHistories() {
        return oldLevelHistories;
    }

    public void setOldLevelHistories(List<MembershipHistory> oldLevelHistories) {
        this.oldLevelHistories = oldLevelHistories;
    }
}

