package g6.fashionFlex.service;

import g6.fashionFlex.dto.UserDTO;
import g6.fashionFlex.entity.Role;
import g6.fashionFlex.entity.User;
import g6.fashionFlex.repository.RoleRepository;
import g6.fashionFlex.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class AdminUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Page<UserDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::convertToDTO);
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return convertToDTO(user);
    }

    public UserDTO toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        user.setEnabled(!user.isEnabled());
        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }

    public UserDTO assignRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        user.getRoles().add(role);
        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }

    public UserDTO removeRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        user.getRoles().remove(role);
        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        userRepository.delete(user);
    }

    public UserDTO updateUser(Long id, UserDTO userDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        user.setFullName(userDTO.getFullName());
        user.setPhoneNumber(userDTO.getPhoneNumber());

        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }

    public long countTotalUsers() {
        return userRepository.count();
    }

    public long countUsersByRole(String roleName) {
        return userRepository.findAll().stream()
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> role.getName().equals(roleName)))
                .count();
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setEnabled(user.isEnabled());
        dto.setProvider(user.getProvider());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setRoles(user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet()));
        return dto;
    }

    /**
     * Advanced search with multiple filters
     */
    public Page<UserDTO> searchUsersAdvanced(String keyword, String role, Boolean enabled, Pageable pageable) {
        List<User> allUsers = userRepository.findAll();

        // Apply filters
        List<User> filteredUsers = allUsers.stream()
                .filter(user -> {
                    // Keyword filter (name or email)
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        String lowerKeyword = keyword.toLowerCase();
                        boolean matchesName = user.getFullName().toLowerCase().contains(lowerKeyword);
                        boolean matchesEmail = user.getEmail().toLowerCase().contains(lowerKeyword);
                        if (!matchesName && !matchesEmail) {
                            return false;
                        }
                    }

                    // Role filter
                    if (role != null && !role.trim().isEmpty()) {
                        boolean hasRole = user.getRoles().stream()
                                .anyMatch(r -> r.getName().equalsIgnoreCase(role));
                        if (!hasRole) {
                            return false;
                        }
                    }

                    // Enabled status filter
                    if (enabled != null && user.isEnabled() != enabled) {
                        return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());

        // Convert to Page
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredUsers.size());
        List<UserDTO> pageContent = filteredUsers.subList(start, end).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new org.springframework.data.domain.PageImpl<>(
                pageContent, pageable, filteredUsers.size());
    }

    /**
     * Get user statistics for dashboard
     */
    public UserStatistics getStatistics() {
        List<User> allUsers = userRepository.findAll();

        long totalUsers = allUsers.size();
        long activeUsers = allUsers.stream().filter(User::isEnabled).count();
        long inactiveUsers = allUsers.stream().filter(u -> !u.isEnabled()).count();
        long adminUsers = allUsers.stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN")))
                .count();

        // Users created in last 30 days
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long recentUsers = allUsers.stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(thirtyDaysAgo))
                .count();

        // OAuth users
        long oauthUsers = allUsers.stream()
                .filter(u -> u.getProvider() != null && !u.getProvider().equals("LOCAL"))
                .count();

        return new UserStatistics(totalUsers, activeUsers, inactiveUsers,
                                  adminUsers, recentUsers, oauthUsers);
    }

    /**
     * Bulk activate users
     */
    public int bulkActivate(List<Long> userIds) {
        int count = 0;
        for (Long id : userIds) {
            try {
                User user = userRepository.findById(id).orElse(null);
                if (user != null && !user.isEnabled()) {
                    user.setEnabled(true);
                    userRepository.save(user);
                    count++;
                }
            } catch (Exception e) {
                log.error("Error activating user {}: {}", id, e.getMessage());
            }
        }
        log.info("Bulk activated {} users", count);
        return count;
    }

    /**
     * Bulk deactivate users
     */
    public int bulkDeactivate(List<Long> userIds) {
        int count = 0;
        for (Long id : userIds) {
            try {
                User user = userRepository.findById(id).orElse(null);
                if (user != null && user.isEnabled()) {
                    user.setEnabled(false);
                    userRepository.save(user);
                    count++;
                }
            } catch (Exception e) {
                log.error("Error deactivating user {}: {}", id, e.getMessage());
            }
        }
        log.info("Bulk deactivated {} users", count);
        return count;
    }

    /**
     * Bulk delete users
     */
    public int bulkDelete(List<Long> userIds) {
        int count = 0;
        for (Long id : userIds) {
            try {
                userRepository.deleteById(id);
                count++;
            } catch (Exception e) {
                log.error("Error deleting user {}: {}", id, e.getMessage());
            }
        }
        log.info("Bulk deleted {} users", count);
        return count;
    }

    // User Statistics DTO
    public static class UserStatistics {
        private final long totalUsers;
        private final long activeUsers;
        private final long inactiveUsers;
        private final long adminUsers;
        private final long recentUsers;
        private final long oauthUsers;

        public UserStatistics(long totalUsers, long activeUsers, long inactiveUsers,
                             long adminUsers, long recentUsers, long oauthUsers) {
            this.totalUsers = totalUsers;
            this.activeUsers = activeUsers;
            this.inactiveUsers = inactiveUsers;
            this.adminUsers = adminUsers;
            this.recentUsers = recentUsers;
            this.oauthUsers = oauthUsers;
        }

        public long getTotalUsers() { return totalUsers; }
        public long getActiveUsers() { return activeUsers; }
        public long getInactiveUsers() { return inactiveUsers; }
        public long getAdminUsers() { return adminUsers; }
        public long getRecentUsers() { return recentUsers; }
        public long getOauthUsers() { return oauthUsers; }
    }
}
