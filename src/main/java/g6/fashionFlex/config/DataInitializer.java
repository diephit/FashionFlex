package g6.fashionFlex.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import g6.fashionFlex.entity.Role;
import g6.fashionFlex.entity.Role.RoleName;
import g6.fashionFlex.repository.RoleRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        // Initialize default roles if they don't exist (names: customer, admin)
        if (roleRepository.findByRoleName(RoleName.customer).isEmpty()) {
            Role customer = new Role();
            customer.setRoleName(RoleName.customer);
            roleRepository.save(customer);
            System.out.println("Created default role: customer");
        }

        if (roleRepository.findByRoleName(RoleName.admin).isEmpty()) {
            Role admin = new Role();
            admin.setRoleName(RoleName.admin);
            roleRepository.save(admin);
            System.out.println("Created default role: admin");
        }
    }
}
