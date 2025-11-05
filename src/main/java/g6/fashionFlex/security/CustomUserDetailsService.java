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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        String authority = "ROLE_" + user.getRole().getRoleName().name().toUpperCase();
        Set<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority(authority));

        boolean disabled = user.getStatus() != User.UserStatus.active;

        String presentedPassword = "{noop}" + (user.getPassword() == null ? "" : user.getPassword());

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

        String presentedPassword = "{noop}" + (user.getPassword() == null ? "" : user.getPassword());

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
