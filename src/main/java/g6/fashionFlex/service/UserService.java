package g6.fashionFlex.service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import g6.fashionFlex.dto.RegisterRequest;
import g6.fashionFlex.dto.UserDTO;
import g6.fashionFlex.entity.Customer;
import g6.fashionFlex.entity.MembershipLevel;
import g6.fashionFlex.entity.Role;
import g6.fashionFlex.entity.Role.RoleName;
import g6.fashionFlex.entity.User;
import g6.fashionFlex.exception.ResourceNotFoundException;
import g6.fashionFlex.exception.UserAlreadyExistsException;
import g6.fashionFlex.repository.CustomerRepository;
import g6.fashionFlex.repository.MembershipLevelRepository;
import g6.fashionFlex.repository.RoleRepository;
import g6.fashionFlex.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private MembershipLevelRepository membershipLevelRepository;

    @Transactional
    public UserDTO registerUser(RegisterRequest registerRequest) {
        // Normalize email to lowercase for consistency
        String normalizedEmail = registerRequest.getEmail() != null ? 
                                 registerRequest.getEmail().toLowerCase().trim() : "";
        
        if (normalizedEmail.isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        
        // Double check: Check if email already exists in database
        // This is a safety check even though controller already checked
        boolean emailExists = userRepository.existsByEmail(normalizedEmail);
        
        if (!emailExists) {
            // Also check with findByEmail to be absolutely sure
            emailExists = userRepository.findByEmail(normalizedEmail).isPresent();
        }
        
        if (emailExists) {
            throw new UserAlreadyExistsException("This email has been used, please try another one");
        }

        // Create new user
        User user = new User();
        user.setName(registerRequest.getFullName());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setStatus(User.UserStatus.active);

        // Assign default role (customer)
        Role userRole = roleRepository.findByRoleName(RoleName.customer)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setRoleName(RoleName.customer);
                    return roleRepository.save(newRole);
                });

        user.setRole(userRole);

        // Save user
        User savedUser = userRepository.save(user);

        // Ensure customer profile is created for the new user
        Customer customer = new Customer();
        customer.setUser(savedUser);
        customer.setLoyaltyPoints(0);
        customer.setTotalSpent(BigDecimal.ZERO);

        // Assign default membership level (Bronze / lowest minSpent)
        Optional<MembershipLevel> defaultLevel = membershipLevelRepository.findByLevelName("Bronze");
        if (defaultLevel.isEmpty()) {
            defaultLevel = membershipLevelRepository.findAllOrderByMinSpentAsc().stream().findFirst();
        }
        defaultLevel.ifPresent(customer::setLevel);

        customerRepository.save(customer);

        // Convert to DTO and return
        return convertToDTO(savedUser);
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id == null ? null : Integer.valueOf(id.intValue()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return convertToDTO(user);
    }

    @Transactional(readOnly = true)
    public UserDTO getUserByEmail(String email) {
        // Normalize email to lowercase for case-insensitive lookup
        String normalizedEmail = email.toLowerCase().trim();
        
        // Try normalized email first, then fallback to original
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> userRepository.findByEmail(email)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email)));
        return convertToDTO(user);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getUserID() == null ? null : user.getUserID().longValue());
        dto.setFullName(user.getName());
        dto.setEmail(user.getEmail());
        // Fields not present in the new schema are left null (phone, address, provider)
        dto.setEnabled(user.getStatus() == User.UserStatus.active);
        dto.setCreatedAt(user.getCreatedAt());

        Set<String> roleNames = new HashSet<>();
        roleNames.add("ROLE_" + user.getRole().getRoleName().name().toUpperCase());
        dto.setRoles(roleNames);

        return dto;
    }

    @Transactional
    public User createOrUpdateOAuth2User(String provider, String providerId, String email, String fullName) {
        // Since provider fields are not part of the new schema, fall back to email-based lookup
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setName(fullName);
                    newUser.setPassword(passwordEncoder.encode("OAUTH2_USER_" + System.currentTimeMillis()));
                    newUser.setStatus(User.UserStatus.active);

                    Role userRole = roleRepository.findByRoleName(RoleName.customer)
                            .orElseGet(() -> {
                                Role r = new Role();
                                r.setRoleName(RoleName.customer);
                                return roleRepository.save(r);
                            });

                    newUser.setRole(userRole);
                    return userRepository.save(newUser);
                });
    }
}
