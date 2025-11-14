package g6.fashionFlex.security;

import java.util.Collections;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import g6.fashionFlex.entity.User;
import g6.fashionFlex.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Normalize email to lowercase for case-insensitive lookup
        String normalizedEmail = email.toLowerCase().trim();
        
        // Try to find user with normalized email first
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> {
                    // Fallback: try original email (for backward compatibility)
                    return userRepository.findByEmail(email)
                            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
                });

        String authority = "ROLE_" + user.getRole().getRoleName().name().toUpperCase();
        Set<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority(authority));

        boolean disabled = user.getStatus() != User.UserStatus.active;

        // Password should already be encoded with prefix (e.g., {bcrypt}...) from registration
        // If it doesn't have a prefix, it means it's plain text (old data), add {noop} prefix
        String userPassword = user.getPassword() == null ? "" : user.getPassword();
        String presentedPassword = userPassword;
        if (!userPassword.isEmpty() && !userPassword.startsWith("{")) {
            // Password is plain text, add {noop} prefix for backward compatibility
            presentedPassword = "{noop}" + userPassword;
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(presentedPassword)
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(disabled)
                .build();
    }

    @Transactional
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id == null ? null : Integer.valueOf(id.intValue()))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

        String authority = "ROLE_" + user.getRole().getRoleName().name().toUpperCase();
        Set<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority(authority));

        boolean disabled = user.getStatus() != User.UserStatus.active;

        // Use password as-is if it already has encoding prefix (e.g., {bcrypt}, {noop})
        // Otherwise, add {noop} prefix for plain text passwords (backward compatibility)
        String userPassword = user.getPassword() == null ? "" : user.getPassword();
        String presentedPassword = userPassword;
        if (!userPassword.isEmpty() && !userPassword.startsWith("{")) {
            // Password is plain text, add {noop} prefix
            presentedPassword = "{noop}" + userPassword;
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(presentedPassword)
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(disabled)
                .build();
    }
}
