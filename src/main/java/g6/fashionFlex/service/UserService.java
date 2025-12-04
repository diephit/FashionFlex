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
        String normalizedEmail = registerRequest.getEmail() != null ? 
                                 registerRequest.getEmail().toLowerCase().trim() : "";
        
        if (normalizedEmail.isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        
        boolean emailExists = userRepository.existsByEmail(normalizedEmail);
        
        if (!emailExists) {
            emailExists = userRepository.findByEmail(normalizedEmail).isPresent();
        }
        
        if (emailExists) {
            throw new UserAlreadyExistsException("This email has been used, please try another one");
        }

        String normalizedPhone = registerRequest.getPhone() != null ?
                registerRequest.getPhone().trim() : "";

        if (normalizedPhone.isEmpty()) {
            throw new IllegalArgumentException("Phone number is required");
        }

        if (!normalizedPhone.matches("^0\\d{9}$")) {
            throw new IllegalArgumentException("Phone number must start with 0 and contain exactly 10 digits");
        }

        if (customerRepository.existsByPhone(normalizedPhone)) {
            throw new IllegalArgumentException("This phone number has been used, please try another one");
        }

        User user = new User();
        user.setName(registerRequest.getFullName());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setStatus(User.UserStatus.active);

        Role userRole = roleRepository.findByRoleName(RoleName.customer)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setRoleName(RoleName.customer);
                    return roleRepository.save(newRole);
                });

        user.setRole(userRole);
        User savedUser = userRepository.save(user);

        createCustomerProfile(savedUser, normalizedPhone);

        return convertToDTO(savedUser);
    }

    @Transactional
    public User createOrUpdateOAuth2User(String provider, String providerId, String email, String fullName) {
        String normalizedEmail = email.toLowerCase().trim();
        
        Optional<User> existingUser = userRepository.findByEmail(normalizedEmail);
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (fullName != null && !fullName.isBlank() && !fullName.equals(user.getName())) {
                user.setName(fullName);
                userRepository.save(user);
            }
            return user;
        }
        
        User newUser = new User();
        newUser.setEmail(normalizedEmail);
        newUser.setName(fullName);
        // OAuth-based signups start with a placeholder password so the DB constraint is satisfied.
        newUser.setPassword(PasswordSetupService.buildPlaceholderPassword());
        newUser.setStatus(User.UserStatus.active);

        Role userRole = roleRepository.findByRoleName(RoleName.customer)
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setRoleName(RoleName.customer);
                    return roleRepository.save(r);
                });

        newUser.setRole(userRole);
        User savedUser = userRepository.save(newUser);
        
        createCustomerProfile(savedUser, null);
        
        return savedUser;
    }

    private Customer createCustomerProfile(User user, String phone) {
        Optional<Customer> existingCustomer = customerRepository.findByUser(user);
        if (existingCustomer.isPresent()) {
            Customer customer = existingCustomer.get();
            if (phone != null && !phone.isBlank() && (customer.getPhone() == null || !customer.getPhone().equals(phone))) {
                customer.setPhone(phone);
                return customerRepository.save(customer);
            }
            return customer;
        }
        
        Customer customer = new Customer();
        customer.setUser(user);
        customer.setLoyaltyPoints(0);
        customer.setTotalSpent(BigDecimal.ZERO);
        if (phone != null && !phone.isBlank()) {
            customer.setPhone(phone);
        }

        Optional<MembershipLevel> defaultLevel = membershipLevelRepository.findByLevelName("Bronze");
        if (defaultLevel.isEmpty()) {
            defaultLevel = membershipLevelRepository.findAllOrderByMinSpentAsc().stream().findFirst();
        }
        defaultLevel.ifPresent(customer::setLevel);

        return customerRepository.save(customer);
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id == null ? null : Integer.valueOf(id.intValue()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return convertToDTO(user);
    }

    @Transactional(readOnly = true)
    public UserDTO getUserByEmail(String email) {
        String normalizedEmail = email.toLowerCase().trim();
        
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
        dto.setEnabled(user.getStatus() == User.UserStatus.active);
        dto.setCreatedAt(user.getCreatedAt());

        Set<String> roleNames = new HashSet<>();
        roleNames.add("ROLE_" + user.getRole().getRoleName().name().toUpperCase());
        dto.setRoles(roleNames);

        return dto;
    }
}