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

    private static final String[] WALLET_NAMES = {"Cash Wallet", "Bank Account", "Credit Card"};

    @PostConstruct
    public void seedData() {
        System.out.println("[DataSeeder] Starting seed process...");

        // Tạo Test User nếu chưa tồn tại
        User testUser = null;
        if (userRepository.findByUsername("test123@example.com").isEmpty()) {
            System.out.println("[DataSeeder] Creating test user...");
            testUser = new User();
            testUser.setFirstName("Test");
            testUser.setLastName("User");
            testUser.setEmail("test123@example.com");
            testUser.setUsername("test123@example.com");
            testUser.setPassword(passwordEncoder.encode("Test@123"));
            testUser.setRoles(Set.of("USER"));
            testUser.setEnabled(true);
            userRepository.save(testUser);
            System.out.println("[DataSeeder] Test user saved.");

            // Tạo Categories cho test user
            System.out.println("[DataSeeder] Creating categories for test user...");
            List<Category> categories = Arrays.asList(
                    new Category("Food & Dining", testUser),
                    new Category("Transportation", testUser),
                    new Category("Entertainment", testUser),
                    new Category("Housing", testUser),
                    new Category("Utilities", testUser),
                    new Category("Shopping", testUser),
                    new Category("Salary", testUser),
                    new Category("Freelance", testUser),
                    new Category("Gifts", testUser),
                    new Category("Investments", testUser)
            );

            categoryRepository.saveAll(categories);
            System.out.println("[DataSeeder] Categories saved for test user.");

            // Tạo Wallets và Transactions cho test user
            Random random = new Random();

            for (String walletName : WALLET_NAMES) {
                System.out.println("[DataSeeder] Creating wallet: " + walletName + " for test user");
                Wallet wallet = new Wallet(walletName, BigDecimal.ZERO, testUser);
                walletRepository.save(wallet);

                int transactionCount = random.nextInt(11) + 20; // 20-30 transactions
                System.out.println("[DataSeeder] Creating " + transactionCount + " transactions for wallet: " + walletName);

                List<Category> allCategories = categoryRepository.findAllByUser(testUser);

                for (int i = 0; i < transactionCount; i++) {
                    Category category = allCategories.get(random.nextInt(allCategories.size()));
                    boolean isExpense = random.nextBoolean();
                    BigDecimal amount = new BigDecimal(random.nextInt(900_001) + 100_000);

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
        } else {
            System.out.println("[DataSeeder] Test user already exists. Skipping test user seeding.");
        }

        // Tạo categories và wallets cho user mới
        List<User> allUsers = userRepository.findAll();
        System.out.println("[DataSeeder] Found " + allUsers.size() + " users.");

        for (User currentUser : allUsers) {
            System.out.println("[DataSeeder] Processing user: " + currentUser.getUsername());

            // Kiểm tra và seed categories
            List<Category> userCategories = categoryRepository.findAllByUser(currentUser);
            if (userCategories.isEmpty()) {
                System.out.println("[DataSeeder] Creating categories for user: " + currentUser.getUsername());
                List<Category> newCategories = Arrays.asList(
                        new Category("Food & Dining", currentUser),
                        new Category("Transportation", currentUser),
                        new Category("Entertainment", currentUser),
                        new Category("Housing", currentUser),
                        new Category("Utilities", currentUser),
                        new Category("Shopping", currentUser),
                        new Category("Salary", currentUser),
                        new Category("Freelance", currentUser),
                        new Category("Gifts", currentUser),
                        new Category("Investments", currentUser)
                );
                categoryRepository.saveAll(newCategories);
                System.out.println("[DataSeeder] Categories saved for user: " + currentUser.getUsername());
            } else {
                System.out.println("[DataSeeder] Categories already exist for user: " + currentUser.getUsername());
            }

            // Ktra và seed wallets
            List<Wallet> userWallets = walletRepository.findAllByUser(currentUser);
            if (userWallets.isEmpty()) {
                System.out.println("[DataSeeder] Creating wallets for user: " + currentUser.getUsername());
                for (String walletName : WALLET_NAMES) {
                    System.out.println("[DataSeeder] Creating wallet: " + walletName + " for user: " + currentUser.getUsername());
                    Wallet wallet = new Wallet(walletName, BigDecimal.ZERO, currentUser);
                    walletRepository.save(wallet);
                }
                System.out.println("[DataSeeder] Wallets saved for user: " + currentUser.getUsername());
            } else {
                System.out.println("[DataSeeder] Wallets already exist for user: " + currentUser.getUsername());
            }
        }

        System.out.println("[DataSeeder] Seeding process completed.");
    }
}