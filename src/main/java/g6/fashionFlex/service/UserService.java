package g6.fashionFlex.service;

import g6.fashionFlex.dto.ChangePasswordRequest;
import g6.fashionFlex.dto.RegisterRequest;
import g6.fashionFlex.dto.UpdateProfileRequest;
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
        user.setHasSetPassword(true); // Local users register with a password

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
        dto.setHasSetPassword(user.isHasSetPassword());
        dto.setProfileImageUrl(user.getProfileImageUrl());
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
                    newUser.setHasSetPassword(false); // OAuth2 users haven't set a custom password yet

                    // Assign default role
                    Role userRole = roleRepository.findByName("ROLE_USER")
                            .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));

                    Set<Role> roles = new HashSet<>();
                    roles.add(userRole);
                    newUser.setRoles(roles);

                    return userRepository.save(newUser);
                });
    }

    @Transactional
    public UserDTO updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Check if email is being changed and if new email already exists
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already in use: " + request.getEmail());
        }

        // Update user information
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());

        // Update profile image if provided
        if (request.getProfileImageUrl() != null && !request.getProfileImageUrl().isEmpty()) {
            user.setProfileImageUrl(request.getProfileImageUrl());
        }

        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Validate that new password and confirm password match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        // Check if user is OAuth2 user who hasn't set a password yet
        boolean isOAuth2User = user.getProvider() != null && !user.getProvider().equals("local");
        boolean hasNotSetPassword = !user.isHasSetPassword();

        // For OAuth2 users setting password for the first time:
        // Allow empty/null current password
        if (isOAuth2User && hasNotSetPassword && (request.getCurrentPassword() == null || request.getCurrentPassword().isEmpty())) {
            // OAuth2 user is setting password for the first time
            // No need to verify current password (temporary OAuth2 password)
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            // Mark that user has set a custom password
            user.setHasSetPassword(true);
            // Keep provider as is (google, facebook, etc.) so they can still login via OAuth2
        } else {
            // For local users or OAuth2 users who have already set a password:
            // Require current password verification
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isEmpty()) {
                throw new IllegalArgumentException("Current password is required");
            }

            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new IllegalArgumentException("Current password is incorrect");
            }

            // Update password
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));

            // Mark as having set password (in case it wasn't already)
            if (!user.isHasSetPassword()) {
                user.setHasSetPassword(true);
            }
        }

        userRepository.save(user);
    }
}
