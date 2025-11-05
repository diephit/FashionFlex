package g6.fashionFlex.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import g6.fashionFlex.dto.RegisterRequest;
import g6.fashionFlex.dto.UserDTO;
import g6.fashionFlex.entity.Role;
import g6.fashionFlex.entity.Role.RoleName;
import g6.fashionFlex.entity.User;
import g6.fashionFlex.exception.ResourceNotFoundException;
import g6.fashionFlex.exception.UserAlreadyExistsException;
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

    @Transactional
    public UserDTO registerUser(RegisterRequest registerRequest) {
        // Check if email already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered: " + registerRequest.getEmail());
        }

        // Create new user
        User user = new User();
        user.setName(registerRequest.getFullName());
        user.setEmail(registerRequest.getEmail());
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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
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
