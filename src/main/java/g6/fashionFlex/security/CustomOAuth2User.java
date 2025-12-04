package g6.fashionFlex.security;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import g6.fashionFlex.entity.User;
import g6.fashionFlex.entity.Role;
import g6.fashionFlex.repository.RoleRepository;
import g6.fashionFlex.service.UserService;

@Component
public class CustomOAuth2User extends DefaultOAuth2UserService {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2User.class);

    @Autowired
    private UserService userService;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        processOAuth2User(userRequest, oauth2User);
        
        return oauth2User;
    }

    private void processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oauth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oauth2User.getAttributes();
        
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String providerId = (String) attributes.get("sub");
        
        if (email == null || email.isEmpty()) {
            logger.error("Email not found from OAuth2 provider");
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }
        
        email = email.toLowerCase().trim();
        
        logger.info("Processing OAuth2 user: email={}, provider={}", email, registrationId);
        
        User user = userService.createOrUpdateOAuth2User(registrationId, providerId, email, name);
        
        if (user == null) {
            logger.error("Failed to process OAuth2 user: {}", email);
            throw new OAuth2AuthenticationException("Failed to process OAuth2 user");
        }
        
        logger.info("Successfully processed OAuth2 user: {}", email);
    }
}