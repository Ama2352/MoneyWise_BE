package JavaProject.MoneyWise.config;

import JavaProject.MoneyWise.models.entities.Budget;
import JavaProject.MoneyWise.models.entities.Category;
import JavaProject.MoneyWise.models.entities.SavingGoal;
import JavaProject.MoneyWise.models.entities.Transaction;
import JavaProject.MoneyWise.models.entities.User;
import JavaProject.MoneyWise.models.entities.Wallet;
import JavaProject.MoneyWise.repositories.BudgetRepository;
import JavaProject.MoneyWise.repositories.CategoryRepository;
import JavaProject.MoneyWise.repositories.SavingGoalRepository;
import JavaProject.MoneyWise.repositories.TransactionRepository;
import JavaProject.MoneyWise.repositories.UserRepository;
import JavaProject.MoneyWise.repositories.WalletRepository;
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
    private final BudgetRepository budgetRepository;
    private final SavingGoalRepository savingGoalRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String[] WALLET_NAMES = {"Cash Wallet", "Bank Account", "Credit Card"};

    @PostConstruct
    public void seedData() {
        System.out.println("[DataSeeder] Starting seed process...");

        // Tạo Test User nếu chưa tồn tại
        User testUser = null;
        if (userRepository.findByUsername("test1234@example.com").isEmpty()) {
            System.out.println("[DataSeeder] Creating test user...");
            testUser = new User();
            testUser.setFirstName("Test");
            testUser.setLastName("User");
            testUser.setEmail("test1234@example.com");
            testUser.setUsername("test1234@example.com");
            testUser.setPassword(passwordEncoder.encode("Test@1234"));
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

            // Lấy categories để sử dụng
            List<Category> allCategories = categoryRepository.findAllByUser(testUser);
            Category foodCategory = allCategories.stream()
                    .filter(c -> c.getName().equals("Food & Dining")).findFirst().orElse(null);
            Category investmentCategory = allCategories.stream()
                    .filter(c -> c.getName().equals("Investments")).findFirst().orElse(null);

            // Tạo Wallets và Transactions cho test user
            Random random = new Random();

            for (String walletName : WALLET_NAMES) {
                System.out.println("[DataSeeder] Creating wallet: " + walletName + " for test user");
                Wallet wallet = new Wallet(walletName, BigDecimal.ZERO, testUser);
                walletRepository.save(wallet);

                int transactionCount = random.nextInt(11) + 20; // 20-30 transactions
                System.out.println("[DataSeeder] Creating " + transactionCount + " transactions for wallet: " + walletName);

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
                    BigDecimal balanceChange = isExpense ? amount.negate() : amount;
                    wallet.setBalance(wallet.getBalance().add(balanceChange));
                }

                walletRepository.save(wallet);
                System.out.println("[DataSeeder] Wallet saved with final balance: " + wallet.getBalance());

                // Tạo Budgets cho từng wallet để kiểm tra các trạng thái
                if (foodCategory != null) {
                    createBudgetsForTesting(wallet, foodCategory);
                }

                // Tạo Saving Goals cho từng wallet để kiểm tra các trạng thái
                if (investmentCategory != null) {
                    createSavingGoalsForTesting(wallet, investmentCategory);
                }
            }
        } else {
            System.out.println("[DataSeeder] Test user already exists. Skipping test user seeding.");
        }

        System.out.println("[DataSeeder] Seeding process completed.");
    }

    private void createBudgetsForTesting(Wallet wallet, Category category) {
        LocalDateTime now = LocalDateTime.now();
        Random random = new Random();

        // Budget 1: Not Started (start date in future)
        Budget notStarted = new Budget();
        notStarted.setWallet(wallet);
        notStarted.setCategory(category);
        notStarted.setDescription("Not Started Budget");
        notStarted.setLimitAmount(new BigDecimal("1000000"));
        notStarted.setCurrentSpending(BigDecimal.ZERO);
        notStarted.setStartDate(now.plusDays(10));
        notStarted.setEndDate(now.plusDays(40));
        budgetRepository.save(notStarted);

        // Budget 2: Over Budget (ended, usage > 100%)
        Budget overBudget = new Budget();
        overBudget.setWallet(wallet);
        overBudget.setCategory(category);
        overBudget.setDescription("Over Budget");
        overBudget.setLimitAmount(new BigDecimal("500000"));
        overBudget.setCurrentSpending(new BigDecimal("600000"));
        overBudget.setStartDate(now.minusDays(60));
        overBudget.setEndDate(now.minusDays(30));
        budgetRepository.save(overBudget);
        createTransaction(wallet, category, new BigDecimal("600000"), "expense",
                now.minusDays(45), "Over budget transaction");

        // Budget 3: Nearly Maxed (ended, 90% < usage ≤ 100%)
        Budget nearlyMaxed = new Budget();
        nearlyMaxed.setWallet(wallet);
        nearlyMaxed.setCategory(category);
        nearlyMaxed.setDescription("Nearly Maxed Budget");
        nearlyMaxed.setLimitAmount(new BigDecimal("500000"));
        nearlyMaxed.setCurrentSpending(new BigDecimal("460000"));
        nearlyMaxed.setStartDate(now.minusDays(60));
        nearlyMaxed.setEndDate(now.minusDays(30));
        budgetRepository.save(nearlyMaxed);
        createTransaction(wallet, category, new BigDecimal("460000"), "expense",
                now.minusDays(45), "Nearly maxed transaction");

        // Budget 4: Under Budget (ended, usage ≤ 90%)
        Budget underBudgetEnded = new Budget();
        underBudgetEnded.setWallet(wallet);
        underBudgetEnded.setCategory(category);
        underBudgetEnded.setDescription("Under Budget (Ended)");
        underBudgetEnded.setLimitAmount(new BigDecimal("500000"));
        underBudgetEnded.setCurrentSpending(new BigDecimal("300000"));
        underBudgetEnded.setStartDate(now.minusDays(60));
        underBudgetEnded.setEndDate(now.minusDays(30));
        budgetRepository.save(underBudgetEnded);
        createTransaction(wallet, category, new BigDecimal("300000"), "expense",
                now.minusDays(45), "Under budget transaction");

        // Budget 5: Critical (active, spending ratio > 1.5)
        Budget critical = new Budget();
        critical.setWallet(wallet);
        critical.setCategory(category);
        critical.setDescription("Critical Budget");
        critical.setLimitAmount(new BigDecimal("1000000"));
        critical.setCurrentSpending(new BigDecimal("750000")); // 75% spent
        critical.setStartDate(now.minusDays(20));
        critical.setEndDate(now.plusDays(10)); // 30 days total, 20 days elapsed (~66.67% time)
        budgetRepository.save(critical);
        createTransaction(wallet, category, new BigDecimal("750000"), "expense",
                now.minusDays(10), "Critical budget transaction");

        // Budget 6: Warning (active, 1.2 < spending ratio ≤ 1.5)
        Budget warning = new Budget();
        warning.setWallet(wallet);
        warning.setCategory(category);
        warning.setDescription("Warning Budget");
        warning.setLimitAmount(new BigDecimal("1000000"));
        warning.setCurrentSpending(new BigDecimal("600000")); // 60% spent
        warning.setStartDate(now.minusDays(20));
        warning.setEndDate(now.plusDays(10));
        budgetRepository.save(warning);
        createTransaction(wallet, category, new BigDecimal("600000"), "expense",
                now.minusDays(10), "Warning budget transaction");

        // Budget 7: On Track (active, 0.8 ≤ spending ratio ≤ 1.2)
        Budget onTrack = new Budget();
        onTrack.setWallet(wallet);
        onTrack.setCategory(category);
        onTrack.setDescription("On Track Budget");
        onTrack.setLimitAmount(new BigDecimal("1000000"));
        onTrack.setCurrentSpending(new BigDecimal("500000")); // 50% spent
        onTrack.setStartDate(now.minusDays(20));
        onTrack.setEndDate(now.plusDays(10));
        budgetRepository.save(onTrack);
        createTransaction(wallet, category, new BigDecimal("500000"), "expense",
                now.minusDays(10), "On track budget transaction");

        // Budget 8: Under Budget (active, 0.5 ≤ spending ratio < 0.8)
        Budget underBudget = new Budget();
        underBudget.setWallet(wallet);
        underBudget.setCategory(category);
        underBudget.setDescription("Under Budget (Active)");
        underBudget.setLimitAmount(new BigDecimal("1000000"));
        underBudget.setCurrentSpending(new BigDecimal("400000")); // 40% spent
        underBudget.setStartDate(now.minusDays(20));
        underBudget.setEndDate(now.plusDays(10));
        budgetRepository.save(underBudget);
        createTransaction(wallet, category, new BigDecimal("400000"), "expense",
                now.minusDays(10), "Under budget active transaction");

        // Budget 9: Minimal Spending (active, spending ratio < 0.5)
        Budget minimalSpending = new Budget();
        minimalSpending.setWallet(wallet);
        minimalSpending.setCategory(category);
        minimalSpending.setDescription("Minimal Spending Budget");
        minimalSpending.setLimitAmount(new BigDecimal("1000000"));
        minimalSpending.setCurrentSpending(new BigDecimal("200000")); // 20% spent
        minimalSpending.setStartDate(now.minusDays(20));
        minimalSpending.setEndDate(now.plusDays(10));
        budgetRepository.save(minimalSpending);
        createTransaction(wallet, category, new BigDecimal("200000"), "expense",
                now.minusDays(10), "Minimal spending transaction");
    }

    private void createSavingGoalsForTesting(Wallet wallet, Category category) {
        LocalDateTime now = LocalDateTime.now();
        Random random = new Random();

        // Saving Goal 1: Not Started (start date in future)
        SavingGoal notStarted = new SavingGoal();
        notStarted.setWallet(wallet);
        notStarted.setCategory(category);
        notStarted.setDescription("Not Started Saving Goal");
        notStarted.setTargetAmount(new BigDecimal("1000000"));
        notStarted.setSavedAmount(BigDecimal.ZERO);
        notStarted.setStartDate(now.plusDays(10));
        notStarted.setEndDate(now.plusDays(40));
        savingGoalRepository.save(notStarted);

        // Saving Goal 2: Achieved (ended, saved ≥ 100%)
        SavingGoal achieved = new SavingGoal();
        achieved.setWallet(wallet);
        achieved.setCategory(category);
        achieved.setDescription("Achieved Saving Goal");
        achieved.setTargetAmount(new BigDecimal("500000"));
        achieved.setSavedAmount(new BigDecimal("500000"));
        achieved.setStartDate(now.minusDays(60));
        achieved.setEndDate(now.minusDays(30));
        savingGoalRepository.save(achieved);
        createTransaction(wallet, category, new BigDecimal("500000"), "income",
                now.minusDays(45), "Achieved saving goal transaction");

        // Saving Goal 3: Partially Achieved (ended, 75% ≤ saved < 100%)
        SavingGoal partiallyAchieved = new SavingGoal();
        partiallyAchieved.setWallet(wallet);
        partiallyAchieved.setCategory(category);
        partiallyAchieved.setDescription("Partially Achieved Saving Goal");
        partiallyAchieved.setTargetAmount(new BigDecimal("500000"));
        partiallyAchieved.setSavedAmount(new BigDecimal("400000"));
        partiallyAchieved.setStartDate(now.minusDays(60));
        partiallyAchieved.setEndDate(now.minusDays(30));
        savingGoalRepository.save(partiallyAchieved);
        createTransaction(wallet, category, new BigDecimal("400000"), "income",
                now.minusDays(45), "Partially achieved saving goal transaction");

        // Saving Goal 4: Missed Target (ended, saved < 75%)
        SavingGoal missedTarget = new SavingGoal();
        missedTarget.setWallet(wallet);
        missedTarget.setCategory(category);
        missedTarget.setDescription("Missed Target Saving Goal");
        missedTarget.setTargetAmount(new BigDecimal("500000"));
        missedTarget.setSavedAmount(new BigDecimal("300000"));
        missedTarget.setStartDate(now.minusDays(60));
        missedTarget.setEndDate(now.minusDays(30));
        savingGoalRepository.save(missedTarget);
        createTransaction(wallet, category, new BigDecimal("300000"), "income",
                now.minusDays(45), "Missed target saving goal transaction");

        // Saving Goal 5: Achieved Early (active, saved ≥ target)
        SavingGoal achievedEarly = new SavingGoal();
        achievedEarly.setWallet(wallet);
        achievedEarly.setCategory(category);
        achievedEarly.setDescription("Achieved Early Saving Goal");
        achievedEarly.setTargetAmount(new BigDecimal("500000"));
        achievedEarly.setSavedAmount(new BigDecimal("500000"));
        achievedEarly.setStartDate(now.minusDays(20));
        achievedEarly.setEndDate(now.plusDays(10));
        savingGoalRepository.save(achievedEarly);
        createTransaction(wallet, category, new BigDecimal("500000"), "income",
                now.minusDays(10), "Achieved early saving goal transaction");

        // Saving Goal 6: Ahead (active, progress ratio > 1.2)
        SavingGoal ahead = new SavingGoal();
        ahead.setWallet(wallet);
        ahead.setCategory(category);
        ahead.setDescription("Ahead Saving Goal");
        ahead.setTargetAmount(new BigDecimal("1000000"));
        ahead.setSavedAmount(new BigDecimal("800000")); // 80% saved
        ahead.setStartDate(now.minusDays(20));
        ahead.setEndDate(now.plusDays(10)); // 30 days total, 20 days elapsed (~66.67% time)
        savingGoalRepository.save(ahead);
        createTransaction(wallet, category, new BigDecimal("800000"), "income",
                now.minusDays(10), "Ahead saving goal transaction");

        // Saving Goal 7: On Track (active, 0.8 ≤ progress ratio ≤ 1.2)
        SavingGoal onTrack = new SavingGoal();
        onTrack.setWallet(wallet);
        onTrack.setCategory(category);
        onTrack.setDescription("On Track Saving Goal");
        onTrack.setTargetAmount(new BigDecimal("1000000"));
        onTrack.setSavedAmount(new BigDecimal("600000")); // 60% saved
        onTrack.setStartDate(now.minusDays(20));
        onTrack.setEndDate(now.plusDays(10));
        savingGoalRepository.save(onTrack);
        createTransaction(wallet, category, new BigDecimal("600000"), "income",
                now.minusDays(10), "On track saving goal transaction");

        // Saving Goal 8: Slightly Behind (active, 0.6 ≤ progress ratio < 0.8)
        SavingGoal slightlyBehind = new SavingGoal();
        slightlyBehind.setWallet(wallet);
        slightlyBehind.setCategory(category);
        slightlyBehind.setDescription("Slightly Behind Saving Goal");
        slightlyBehind.setTargetAmount(new BigDecimal("1000000"));
        slightlyBehind.setSavedAmount(new BigDecimal("500000")); // 50% saved
        slightlyBehind.setStartDate(now.minusDays(20));
        slightlyBehind.setEndDate(now.plusDays(10));
        savingGoalRepository.save(slightlyBehind);
        createTransaction(wallet, category, new BigDecimal("500000"), "income",
                now.minusDays(10), "Slightly behind saving goal transaction");

        // Saving Goal 9: At Risk (active, progress ratio < 0.6)
        SavingGoal atRisk = new SavingGoal();
        atRisk.setWallet(wallet);
        atRisk.setCategory(category);
        atRisk.setDescription("At Risk Saving Goal");
        atRisk.setTargetAmount(new BigDecimal("1000000"));
        atRisk.setSavedAmount(new BigDecimal("300000")); // 30% saved
        atRisk.setStartDate(now.minusDays(20));
        atRisk.setEndDate(now.plusDays(10));
        savingGoalRepository.save(atRisk);
        createTransaction(wallet, category, new BigDecimal("300000"), "income",
                now.minusDays(10), "At risk saving goal transaction");
    }

    private void createTransaction(Wallet wallet, Category category, BigDecimal amount, String type,
                                   LocalDateTime date, String description) {
        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setCategory(category);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setTransactionDate(date);
        transaction.setDescription(description);

        try {
            transactionRepository.save(transaction);
            System.out.printf("[Transaction] %s - %s: %s (%s)%n",
                    date, category.getName(), amount, type);

            // Update wallet balance
            BigDecimal balanceChange = type.equals("expense") ? amount.negate() : amount;
            wallet.setBalance(wallet.getBalance().add(balanceChange));
            walletRepository.save(wallet);

            // Update Budget or SavingGoal via repository methods
            if (type.equals("expense")) {
                budgetRepository.updateCurrentSpending(category, wallet, amount, date);
            } else if (type.equals("income")) {
                savingGoalRepository.updateSavedAmount(category, wallet, amount, date);
            }
        } catch (Exception e) {
            System.err.println("[Error] Saving transaction failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}