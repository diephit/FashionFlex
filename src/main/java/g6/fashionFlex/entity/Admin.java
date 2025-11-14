package g6.fashionFlex.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "admins") 
public class Admin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "adminID")
    private Integer adminID;

    @OneToOne
    @JoinColumn(name = "userID", unique = true)
    private User user;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "position", length = 100)
    private String position;

    @Column(name = "createdAt")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "admin")
    private List<Stock> stocks;

    @OneToMany(mappedBy = "createdByAdmin")
    private List<Product> createdProducts;

    @OneToMany(mappedBy = "updatedByAdmin")
    private List<Product> updatedProducts;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Admin() {
    }

    public Integer getAdminID() {
        return adminID;
    }

    public void setAdminID(Integer adminID) {
        this.adminID = adminID;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public List<Stock> getStocks() {
        return stocks;
    }

    public void setStocks(List<Stock> stocks) {
        this.stocks = stocks;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Product> getCreatedProducts() {
        return createdProducts;
    }

    public void setCreatedProducts(List<Product> createdProducts) {
        this.createdProducts = createdProducts;
    }

    public List<Product> getUpdatedProducts() {
        return updatedProducts;
    }

    public void setUpdatedProducts(List<Product> updatedProducts) {
        this.updatedProducts = updatedProducts;
    }
}
