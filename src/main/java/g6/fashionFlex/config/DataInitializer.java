package g6.fashionFlex.config;

import g6.fashionFlex.entity.Role;
import g6.fashionFlex.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        // Initialize default roles if they don't exist
        if (roleRepository.findByName("ROLE_USER").isEmpty()) {
            roleRepository.save(new Role("ROLE_USER"));
            System.out.println("Created default role: ROLE_USER");
        }

        if (roleRepository.findByName("ROLE_ADMIN").isEmpty()) {
            roleRepository.save(new Role("ROLE_ADMIN"));
            System.out.println("Created default role: ROLE_ADMIN");
        }
    }
}
