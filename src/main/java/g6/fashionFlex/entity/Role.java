package g6.fashionFlex.entity;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "roleID")
    private Integer roleID;

    @Enumerated(EnumType.STRING)
    @Column(name = "roleName", nullable = false, columnDefinition = "ENUM('customer', 'admin')")
    private RoleName roleName;

    @OneToMany(mappedBy = "role")
    private List<User> users;

    public Role() {
    }

    // Compatibility constructor allowing creation from string like "ROLE_USER" or "ROLE_ADMIN"
    public Role(String name) {
        this.roleName = parseRoleName(name);
    }

    public Integer getRoleID() {
        return roleID;
    }

    public void setRoleID(Integer roleID) {
        this.roleID = roleID;
    }

    public RoleName getRoleName() {
        return roleName;
    }

    public void setRoleName(RoleName roleName) {
        this.roleName = roleName;
    }

    public enum RoleName {
        customer, admin
    }

    // Compatibility getter expected by security/service layers
    public String getName() {
        return "ROLE_" + (roleName == null ? "" : roleName.name().toUpperCase());
    }

    private static RoleName parseRoleName(String name) {
        if (name == null) {
            return RoleName.customer;
        }
        String normalized = name;
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }
        normalized = normalized.toLowerCase();
        return "admin".equals(normalized) ? RoleName.admin : RoleName.customer;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}
