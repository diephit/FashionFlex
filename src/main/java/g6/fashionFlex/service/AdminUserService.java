package g6.fashionFlex.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import g6.fashionFlex.entity.Role;
import g6.fashionFlex.entity.Role.RoleName;
import g6.fashionFlex.entity.User;
import g6.fashionFlex.entity.User.UserStatus;
import g6.fashionFlex.repository.RoleRepository;
import g6.fashionFlex.repository.UserRepository;

@Service
public class AdminUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Page<User> getAllUsers(Pageable pageable) {
        // Hiển thị cả customer và admin trong bảng
        return userRepository.findAll(pageable);
    }

    public Page<User> searchUsers(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllUsers(pageable);
        }
        String kw = keyword.trim().toLowerCase();
        List<User> allUsers = userRepository.findAll();
        List<User> filtered = allUsers.stream()
                .filter(u -> {
                    String name = u.getName() != null ? u.getName().toLowerCase() : "";
                    String email = u.getEmail() != null ? u.getEmail().toLowerCase() : "";
                    return name.contains(kw) || email.contains(kw);
                })
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<User> pageContent = start > end ? List.of() : filtered.subList(start, end);
        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    public Optional<User> getUserById(Integer userID) {
        return userRepository.findById(userID);
    }

    @Transactional
    public User toggleUserStatus(Integer userID) {
        User user = userRepository.findById(userID)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Không cho phép toggle status của admin
        if (user.getRole() != null && user.getRole().getRoleName() == RoleName.admin) {
            throw new RuntimeException("Cannot change status of admin accounts");
        }
        
        if (user.getStatus() == UserStatus.active) {
            user.setStatus(UserStatus.inactive);
        } else {
            user.setStatus(UserStatus.active);
        }
        
        return userRepository.save(user);
    }

    @Transactional
    public User assignRole(Integer userID, Integer roleID) {
        User user = userRepository.findById(userID)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Role role = roleRepository.findById(roleID)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        
        user.setRole(role);
        return userRepository.save(user);
    }

    @Transactional
    public User removeRole(Integer userID) {
        User user = userRepository.findById(userID)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setRole(null);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Integer userID) {
        User user = userRepository.findById(userID)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userRepository.delete(user);
    }

    @Transactional
    public User createAdmin(String email, String rawPassword) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        String normalizedEmail = email.toLowerCase().trim();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setName(normalizedEmail.split("@")[0]);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setStatus(UserStatus.active);

        Role adminRole = roleRepository.findByRoleName(RoleName.admin)
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setRoleName(RoleName.admin);
                    return roleRepository.save(r);
                });
        user.setRole(adminRole);

        return userRepository.save(user);
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public long countAllUsers() {
        return userRepository.countCustomers(RoleName.customer);
    }

    public long countActiveUsers() {
        return userRepository.countCustomersByStatus(RoleName.customer, UserStatus.active);
    }

    public long countInactiveUsers() {
        return userRepository.countCustomersByStatus(RoleName.customer, UserStatus.inactive);
    }

    public long countNewUsersThisMonth() {
        LocalDate firstDay = LocalDate.now().withDayOfMonth(1);
        LocalDateTime startOfMonth = firstDay.atStartOfDay();
        return userRepository.countNewCustomersAfter(RoleName.customer, startOfMonth);
    }
}

