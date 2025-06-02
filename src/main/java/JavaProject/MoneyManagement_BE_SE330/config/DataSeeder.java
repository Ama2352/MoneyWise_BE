package JavaProject.MoneyManagement_BE_SE330.config;

import JavaProject.MoneyManagement_BE_SE330.models.entities.Category;
import JavaProject.MoneyManagement_BE_SE330.models.entities.Transaction;
import JavaProject.MoneyManagement_BE_SE330.models.entities.User;
import JavaProject.MoneyManagement_BE_SE330.models.entities.Wallet;
import JavaProject.MoneyManagement_BE_SE330.repositories.CategoryRepository;
import JavaProject.MoneyManagement_BE_SE330.repositories.TransactionRepository;
import JavaProject.MoneyManagement_BE_SE330.repositories.UserRepository;
import JavaProject.MoneyManagement_BE_SE330.repositories.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataSeeder {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void seedData() {
        System.out.println("[DataSeeder] Starting seed process...");

        // Kiểm tra nếu đã có dữ liệu, không tạo lại
        if (userRepository.findByUsername("test123@example.com").isPresent()) {
            System.out.println("[DataSeeder] Users already exist. Skipping seeding.");
            return;
        }

        // Tạo Test User
        System.out.println("[DataSeeder] Creating test user...");
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test123@example.com");
        user.setUsername("test123@example.com");
        user.setPassword(passwordEncoder.encode("Test@123"));
        user.setRoles(Set.of("USER"));
        user.setEnabled(true);
        userRepository.save(user);

        System.out.println("[DataSeeder] Test user saved.");

        // Tạo Categories (single list, no separation of expense/income)
        System.out.println("[DataSeeder] Creating categories...");
        List<Category> categories = Arrays.asList(
                new Category("Food & Dining", user),
                new Category("Transportation", user),
                new Category("Entertainment", user),
                new Category("Housing", user),
                new Category("Utilities", user),
                new Category("Shopping", user),
                new Category("Salary", user),
                new Category("Freelance", user),
                new Category("Gifts", user),
                new Category("Investments", user)
        );

        categoryRepository.saveAll(categories);
        System.out.println("[DataSeeder] Categories saved.");

        List<Category> allCategories = categoryRepository.findAll();

        // Tạo Wallets và Transactions
        Random random = new Random();
        String[] walletNames = {"Cash Wallet", "Bank Account", "Credit Card"};

        for (String walletName : walletNames) {
            System.out.println("[DataSeeder] Creating wallet: " + walletName);
            Wallet wallet = new Wallet(walletName, BigDecimal.ZERO, user);
            walletRepository.save(wallet);

            int transactionCount = random.nextInt(11) + 20; // 20-30 transactions
            System.out.println("[DataSeeder] Creating " + transactionCount + " transactions for wallet: " + walletName);

            for (int i = 0; i < transactionCount; i++) {
                Category category = allCategories.get(random.nextInt(allCategories.size()));
                // Randomly decide if this is an expense or income transaction
                boolean isExpense = random.nextBoolean(); // 50% chance for expense or income

                // Set amount income and expenses same
                BigDecimal amount = new BigDecimal(random.nextInt(900_001) + 100_000); // 100,000 to 1,000,000

                // Random date from 01/01/2024 to current time
                LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
                LocalDateTime endDate = LocalDateTime.now();

                long startEpoch = startDate.toEpochSecond(java.time.ZoneOffset.UTC);
                long endEpoch = endDate.toEpochSecond(java.time.ZoneOffset.UTC);
                long randomEpoch = startEpoch + (long)(random.nextDouble() * (endEpoch - startEpoch));
                LocalDateTime date = LocalDateTime.ofEpochSecond(randomEpoch, 0, java.time.ZoneOffset.UTC);

                Transaction transaction = new Transaction();
                transaction.setWallet(wallet);
                transaction.setCategory(category);
                transaction.setAmount(amount);
                transaction.setType(isExpense ? "expense" : "income");
                transaction.setTransactionDate(date);
                transaction.setDescription(category.getName() + " transaction");

                try {
                    transactionRepository.save(transaction);
                    System.out.printf("[Transaction] %s - %s: %s (%s)%n",
                            date, category.getName(), amount, transaction.getType());
                } catch (Exception e) {
                    System.err.println("[Error] Saving transaction failed: " + e.getMessage());
                    e.printStackTrace();
                }

                // Update wallet balance
                wallet.setBalance(wallet.getBalance().add(amount));
            }

            walletRepository.save(wallet);
            System.out.println("[DataSeeder] Wallet saved with final balance: " + wallet.getBalance());
        }
    }
}