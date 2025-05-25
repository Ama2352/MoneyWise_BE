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
        if (userRepository.count() > 0) {
            System.out.println("[DataSeeder] Users already exist. Skipping seeding.");
            return;
        }

        // Tạo Test User
        System.out.println("[DataSeeder] Creating test user...");
        User user = new User();
        user.setFirstName("Hi");
        user.setLastName("User");
        user.setUsername("hi@example.com");
        user.setEmail("hi@example.com");
        user.setPassword(passwordEncoder.encode("Test@123"));
        user.setRoles(Set.of("USER"));
        user.setEnabled(true);
        userRepository.save(user);
        System.out.println("[DataSeeder] Test user saved.");

        // Tạo Categories
        System.out.println("[DataSeeder] Creating categories...");
        List<Category> expenseCategories = Arrays.asList(
                new Category("Food & Dining", user),
                new Category("Transportation", user),
                new Category("Entertainment", user),
                new Category("Housing", user),
                new Category("Utilities", user),
                new Category("Shopping", user)
        );

        List<Category> incomeCategories = Arrays.asList(
                new Category("Salary", user),
                new Category("Freelance", user),
                new Category("Gifts", user),
                new Category("Investments", user)
        );

        categoryRepository.saveAll(expenseCategories);
        categoryRepository.saveAll(incomeCategories);
        System.out.println("[DataSeeder] Categories saved.");

        List<Category> allCategories = categoryRepository.findAll();

        // Tạo Wallets và Transactions
        Random random = new Random();
        String[] walletNames = {"Cash Wallet", "Bank Account", "Credit Card"};

        for (String walletName : walletNames) {
            System.out.println("[DataSeeder] Creating wallet: " + walletName);
            Wallet wallet = new Wallet(walletName, BigDecimal.ZERO, user);
            walletRepository.save(wallet);

            int transactionCount = random.nextInt(6) + 5; // 5-10
            System.out.println("[DataSeeder] Creating " + transactionCount + " transactions for wallet: " + walletName);

            for (int i = 0; i < transactionCount; i++) {
                Category category = allCategories.get(random.nextInt(allCategories.size()));
                boolean isExpense = expenseCategories.stream().anyMatch(c -> c.getName().equals(category.getName()));

                BigDecimal amount = isExpense
                        ? new BigDecimal(random.nextInt(451) + 50)
                        : new BigDecimal(random.nextInt(901) + 100);

                int day = random.nextInt(31) + 1;
                LocalDateTime date = LocalDateTime.of(2025, 1, day, random.nextInt(24), random.nextInt(60));

                Transaction transaction = new Transaction();
                transaction.setWallet(wallet);
                transaction.setCategory(category);
                transaction.setAmount(amount);
                transaction.setType(isExpense ? "expense" : "income");
                transaction.setTransactionDate(date);
                transaction.setDescription(category.getName() + " transaction");

                try {
                    transactionRepository.save(transaction);
                    System.out.printf("[Transaction] %s - %s: %s%n", date, category.getName(), amount);
                } catch (Exception e) {
                    System.err.println("[Error] Saving transaction failed: " + e.getMessage());
                    e.printStackTrace();
                }

                // Cập nhật balance
                wallet.setBalance(wallet.getBalance().add(isExpense ? amount.negate() : amount));
            }

            walletRepository.save(wallet);
            System.out.println("[DataSeeder] Wallet saved with final balance: " + wallet.getBalance());
        }

        System.out.println("[DataSeeder] Seeding process completed.");
    }
}