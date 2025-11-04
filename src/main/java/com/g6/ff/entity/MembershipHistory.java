package com.g6.ff.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "membership_history")
public class MembershipHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "historyID")
    private Integer historyID;

    @ManyToOne
    @JoinColumn(name = "customerID", nullable = false)
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "oldLevelID")
    private MembershipLevel oldLevel;

    @ManyToOne
    @JoinColumn(name = "newLevelID")
    private MembershipLevel newLevel;

    @Column(name = "changedAt")
    private LocalDateTime changedAt;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @PrePersist
    protected void onCreate() {
        changedAt = LocalDateTime.now();
    }

    public MembershipHistory() {
    }

    public Integer getHistoryID() {
        return historyID;
    }

    public void setHistoryID(Integer historyID) {
        this.historyID = historyID;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public MembershipLevel getOldLevel() {
        return oldLevel;
    }

    public void setOldLevel(MembershipLevel oldLevel) {
        this.oldLevel = oldLevel;
    }

    public MembershipLevel getNewLevel() {
        return newLevel;
    }

    public void setNewLevel(MembershipLevel newLevel) {
        this.newLevel = newLevel;
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
}
