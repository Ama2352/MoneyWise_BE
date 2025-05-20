package JavaProject.MoneyManagement_BE_SE330.config;

import JavaProject.MoneyManagement_BE_SE330.models.entities.User;
import JavaProject.MoneyManagement_BE_SE330.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
public class SeedConfig {

    @Bean
    public CommandLineRunner initAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if(!userRepository.existsByUsername("admin@example.com")) {
                User admin = new User();
                admin.setUsername("admin@example.com");
                admin.setPassword(passwordEncoder.encode("Admin@123"));
                admin.setEmail("admin@example.com");
                admin.setFirstName("admin");
                admin.setLastName("chan");
                admin.setRoles(Set.of("ADMIN"));
                admin.setEnabled(true);
                userRepository.save(admin);
                System.out.println("✅ Admin user created.");
            } else {
                System.out.println("ℹ️ Admin user already exists.");
            }
        };
    }
}
