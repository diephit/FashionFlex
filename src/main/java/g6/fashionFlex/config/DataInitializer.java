package g6.fashionFlex.config;

import g6.fashionFlex.entity.Role;
import g6.fashionFlex.entity.User;
import g6.fashionFlex.repository.RoleRepository;
import g6.fashionFlex.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Initialize default roles if they don't exist
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    Role newRole = new Role("ROLE_USER");
                    return roleRepository.save(newRole);
                });

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> {
                    Role newRole = new Role("ROLE_ADMIN");
                    return roleRepository.save(newRole);
                });

        // Create default admin user if it doesn't exist
        if (userRepository.findByEmail("admin@fashionflex.com").isEmpty()) {
            User admin = new User();
            admin.setFullName("FashionFlex Admin");
            admin.setEmail("admin@fashionflex.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setProvider("local");
            admin.setHasSetPassword(true);

            Set<Role> adminRoles = new HashSet<>();
            adminRoles.add(adminRole);
            adminRoles.add(userRole);
            admin.setRoles(adminRoles);

            userRepository.save(admin);
            System.out.println("Created default admin user: admin@fashionflex.com / admin123");
        }

        System.out.println("Data initialization completed successfully!");
    }
}
