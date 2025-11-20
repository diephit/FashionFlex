package g6.fashionFlex.security;

import g6.fashionFlex.entity.Role;
import g6.fashionFlex.entity.User;
import g6.fashionFlex.repository.RoleRepository;
import g6.fashionFlex.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        // Get provider name (google, facebook, etc)
        String provider = userRequest.getClientRegistration().getRegistrationId();

        // Extract user info based on provider
        String providerId;
        if ("google".equals(provider)) {
            providerId = oauth2User.getAttribute("sub"); // Google uses "sub"
        } else if ("facebook".equals(provider)) {
            providerId = oauth2User.getAttribute("id"); // Facebook uses "id"
        } else {
            providerId = oauth2User.getAttribute("id"); // Default
        }

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        // Check if user already exists
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    // Create new user
                    User newUser = new User();
                    newUser.setFullName(name != null ? name : email);
                    newUser.setEmail(email);
                    newUser.setPassword(passwordEncoder.encode("OAUTH2_" + System.currentTimeMillis()));
                    newUser.setProvider(provider);
                    newUser.setProviderId(providerId);
                    newUser.setEnabled(true);

                    // Assign default role
                    Role userRole = roleRepository.findByName("ROLE_USER")
                            .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));

                    Set<Role> roles = new HashSet<>();
                    roles.add(userRole);
                    newUser.setRoles(roles);

                    return userRepository.save(newUser);
                });

        // Update provider info if user logged in with different provider
        if (user.getProvider() == null || !user.getProvider().equals(provider)) {
            user.setProvider(provider);
            user.setProviderId(providerId);
            userRepository.save(user);
        }

        // Convert roles to authorities
        Set<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toSet());

        // Return CustomOAuth2User with proper authorities
        return new CustomOAuth2User(oauth2User, user, authorities);
    }
}
