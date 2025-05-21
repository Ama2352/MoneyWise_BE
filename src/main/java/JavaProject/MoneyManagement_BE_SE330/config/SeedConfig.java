package JavaProject.MoneyManagement_BE_SE330.config;

import JavaProject.MoneyManagement_BE_SE330.models.entities.Category;
import JavaProject.MoneyManagement_BE_SE330.models.entities.User;
import JavaProject.MoneyManagement_BE_SE330.models.entities.Wallet;
import JavaProject.MoneyManagement_BE_SE330.repositories.CategoryRepository;
import JavaProject.MoneyManagement_BE_SE330.repositories.UserRepository;
import JavaProject.MoneyManagement_BE_SE330.repositories.WalletRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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

    @Bean
    public CommandLineRunner initUserData(UserRepository userRepository, CategoryRepository categoryRepository, WalletRepository walletRepository) {
        return args -> {
            List<User> users = userRepository.findAll();

            for (User user : users) {
                // Check if user has categories
                boolean userHasCategories = categoryRepository.existsByUser(user);
                if (!userHasCategories) {
                    List<Category> categories = List.of(
                            new Category("Food", user),
                            new Category("Transport", user),
                            new Category("Entertainment", user),
                            new Category("Bills", user),
                            new Category("Health", user)
                    );
                    categoryRepository.saveAll(categories);
                    System.out.println("✅ Categories seeded for user " + user.getUsername());
                }

                // Check if user has wallets
                boolean userHasWallets = walletRepository.existsByUser(user);
                if (!userHasWallets) {
                    List<Wallet> wallets = List.of(
                            new Wallet("Cash", new BigDecimal("100.00"), user),
                            new Wallet("Bank Account", new BigDecimal("2500.50"), user),
                            new Wallet("Credit Card", new BigDecimal("0.00"), user),
                            new Wallet("Savings", new BigDecimal("10000.00"), user),
                            new Wallet("Investment", new BigDecimal("15000.75"), user)
                    );
                    walletRepository.saveAll(wallets);
                    System.out.println("✅ Wallets seeded for user " + user.getUsername());
                }
            }
        };
    }



}
