package g6.fashionFlex.service;

import g6.fashionFlex.dto.RegisterRequest;
import g6.fashionFlex.dto.UserDTO;
import g6.fashionFlex.entity.Role;
import g6.fashionFlex.entity.User;
import g6.fashionFlex.exception.ResourceNotFoundException;
import g6.fashionFlex.exception.UserAlreadyExistsException;
import g6.fashionFlex.repository.RoleRepository;
import g6.fashionFlex.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
        user.setFullName(registerRequest.getFullName());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setEnabled(true);
        user.setProvider("local");

        // Assign default role
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    Role newRole = new Role("ROLE_USER");
                    return roleRepository.save(newRole);
                });

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        // Save user
        User savedUser = userRepository.save(user);

        // Convert to DTO and return
        return convertToDTO(savedUser);
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
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
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setAddress(user.getAddress());
        dto.setEnabled(user.isEnabled());
        dto.setProvider(user.getProvider());
        dto.setCreatedAt(user.getCreatedAt());

        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        dto.setRoles(roleNames);

        return dto;
    }

    @Transactional
    public User createOrUpdateOAuth2User(String provider, String providerId, String email, String fullName) {
        return userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setProvider(provider);
                    newUser.setProviderId(providerId);
                    newUser.setEmail(email);
                    newUser.setFullName(fullName);
                    newUser.setPassword(passwordEncoder.encode("OAUTH2_USER_" + System.currentTimeMillis()));
                    newUser.setEnabled(true);

                    // Assign default role
                    Role userRole = roleRepository.findByName("ROLE_USER")
                            .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));

                    Set<Role> roles = new HashSet<>();
                    roles.add(userRole);
                    newUser.setRoles(roles);

                    return userRepository.save(newUser);
                });
    }
}
