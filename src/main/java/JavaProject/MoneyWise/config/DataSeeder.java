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
        private static final String[] CATEGORY_NAMES = {
                "Food & Dining", "Transportation", "Entertainment", "Housing",
                "Utilities", "Shopping", "Salary", "Freelance", "Gifts", "Investments"
        };

        @PostConstruct
        public void seedData() {
                System.out.println("[DataSeeder] Starting seed process...");

                // Tìm hoặc tạo Test User
                User testUser;
                if (userRepository.findByUsername("demo3@example.com").isEmpty()) {
                        System.out.println("[DataSeeder] Creating test user...");
                        testUser = new User();
                        testUser.setFirstName("Demo3");
                        testUser.setLastName("User");
                        testUser.setEmail("demo3@example.com");
                        testUser.setUsername("demo3@example.com");
                        testUser.setPassword(passwordEncoder.encode("Demo3@123"));
                        testUser.setRoles(Set.of("USER"));
                        testUser.setEnabled(true);
                        userRepository.save(testUser);
                        System.out.println("[DataSeeder] Demo user created.");
                } else {
                        testUser = userRepository.findByUsername("demo@example.com").get();
                        System.out.println("[DataSeeder] Demo user already exists. Proceeding with seeding.");
                }

                // Tạo Categories nếu chưa tồn tại
                System.out.println("[DataSeeder] Checking and creating categories...");
                List<Category> existingCategories = categoryRepository.findAllByUser(testUser);
                for (String categoryName : CATEGORY_NAMES) {
                        if (existingCategories.stream().noneMatch(c -> c.getName().equals(categoryName))) {
                                Category category = new Category(categoryName, testUser);
                                categoryRepository.save(category);
                                System.out.println("[DataSeeder] Created category: " + categoryName);
                        } else {
                                System.out.println("[DataSeeder] Category already exists: " + categoryName);
                        }
                }

                // Lấy categories để sử dụng
                List<Category> allCategories = categoryRepository.findAllByUser(testUser);

                // Tạo Wallets nếu chưa tồn tại
                for (String walletName : WALLET_NAMES) {
                        if (walletRepository.findByWalletNameAndUser(walletName, testUser).isEmpty()) {
                                System.out.println("[DataSeeder] Creating wallet: " + walletName);
                                BigDecimal initialBalance;
                                switch (walletName) {
                                        case "Cash Wallet":
                                                initialBalance = new BigDecimal("5000000");
                                                break;
                                        case "Bank Account":
                                                initialBalance = new BigDecimal("20000000");
                                                break;
                                        case "Credit Card":
                                                initialBalance = new BigDecimal("-1000000");
                                                break;
                                        default:
                                                initialBalance = BigDecimal.ZERO;
                                }
                                Wallet wallet = new Wallet(walletName, initialBalance, testUser);
                                walletRepository.save(wallet);
                        } else {
                                System.out.println("[DataSeeder] Wallet already exists: " + walletName);
                        }
                }

                // Lấy wallets để sử dụng
                List<Wallet> wallets = walletRepository.findAllByUser(testUser);
                if (wallets.size() < 3) {
                        System.out.println("[DataSeeder] Not enough wallets found. Ensure all wallets are created.");
                        return;
                }

                // Kiểm tra xem đã có budgets hoặc saving goals chưa
                List<Budget> existingBudgets = budgetRepository.findByWalletUser(testUser);
                List<SavingGoal> existingSavingGoals = savingGoalRepository.findByWalletUser(testUser);

                if (!existingBudgets.isEmpty() || !existingSavingGoals.isEmpty()) {
                        System.out.println("[DataSeeder] Budgets or Saving Goals already exist. Skipping seeding to avoid duplicates.");
                        return;
                }

                // Tạo 9 Budgets với các (wallet, category) pair khác nhau
                createBudgetsForTesting(wallets, allCategories);

                // Tạo 9 Saving Goals với các (wallet, category) pair khác nhau
                createSavingGoalsForTesting(wallets, allCategories);

                System.out.println("[DataSeeder] Seeding process completed.");
        }

        private void createBudgetsForTesting(List<Wallet> wallets, List<Category> categories) {
                LocalDateTime now = LocalDateTime.now();

                // Lấy các category phù hợp cho budgets (expense-related)
                List<Category> expenseCategories = categories.stream()
                        .filter(c -> List.of("Food & Dining", "Transportation", "Entertainment", "Housing", "Utilities", "Shopping").contains(c.getName()))
                        .toList();

                // Đảm bảo có đủ categories
                if (expenseCategories.size() < 6) {
                        System.out.println("[DataSeeder] Not enough expense categories for budgets.");
                        return;
                }

                // Budget 1: Not Started (Cash Wallet, Food & Dining)
                Budget notStarted = new Budget();
                notStarted.setWallet(wallets.get(0));
                notStarted.setCategory(expenseCategories.get(0));
                notStarted.setDescription("Not Started Budget");
                notStarted.setLimitAmount(new BigDecimal("1000000"));
                notStarted.setCurrentSpending(BigDecimal.ZERO);
                notStarted.setStartDate(now.plusDays(10));
                notStarted.setEndDate(now.plusDays(40));
                budgetRepository.save(notStarted);

                // Budget 2: Over Budget (Bank Account, Transportation)
                Budget overBudget = new Budget();
                overBudget.setWallet(wallets.get(1));
                overBudget.setCategory(expenseCategories.get(1));
                overBudget.setDescription("Over Budget");
                overBudget.setLimitAmount(new BigDecimal("500000"));
                overBudget.setCurrentSpending(BigDecimal.ZERO);
                overBudget.setStartDate(now.minusDays(60));
                overBudget.setEndDate(now.minusDays(30));
                budgetRepository.save(overBudget);
                createTransaction(wallets.get(1), expenseCategories.get(1), new BigDecimal("600000"), "expense",
                        now.minusDays(45), "Over budget transaction");

                // Budget 3: Nearly Maxed (Credit Card, Entertainment)
                Budget nearlyMaxed = new Budget();
                nearlyMaxed.setWallet(wallets.get(2));
                nearlyMaxed.setCategory(expenseCategories.get(2));
                nearlyMaxed.setDescription("Nearly Maxed Budget");
                nearlyMaxed.setLimitAmount(new BigDecimal("500000"));
                nearlyMaxed.setCurrentSpending(BigDecimal.ZERO);
                nearlyMaxed.setStartDate(now.minusDays(60));
                nearlyMaxed.setEndDate(now.minusDays(30));
                budgetRepository.save(nearlyMaxed);
                createTransaction(wallets.get(2), expenseCategories.get(2), new BigDecimal("460000"), "expense",
                        now.minusDays(45), "Nearly maxed transaction");

                // Budget 4: Under Budget (Ended) (Cash Wallet, Housing)
                Budget underBudgetEnded = new Budget();
                underBudgetEnded.setWallet(wallets.get(0));
                underBudgetEnded.setCategory(expenseCategories.get(3));
                underBudgetEnded.setDescription("Under Budget (Ended)");
                underBudgetEnded.setLimitAmount(new BigDecimal("500000"));
                underBudgetEnded.setCurrentSpending(BigDecimal.ZERO);
                underBudgetEnded.setStartDate(now.minusDays(60));
                underBudgetEnded.setEndDate(now.minusDays(30));
                budgetRepository.save(underBudgetEnded);
                createTransaction(wallets.get(0), expenseCategories.get(3), new BigDecimal("300000"), "expense",
                        now.minusDays(45), "Under budget transaction");

                // Budget 5: Critical (Bank Account, Utilities)
                Budget critical = new Budget();
                critical.setWallet(wallets.get(1));
                critical.setCategory(expenseCategories.get(4));
                critical.setDescription("Critical Budget");
                critical.setLimitAmount(new BigDecimal("1000000"));
                critical.setCurrentSpending(BigDecimal.ZERO); // 75% spent, ratio > 1.5
                critical.setStartDate(now.minusDays(20));
                critical.setEndDate(now.plusDays(10)); // 30 days total, 20 days elapsed (~66.67% time)
                budgetRepository.save(critical);
                createTransaction(wallets.get(1), expenseCategories.get(4), new BigDecimal("750000"), "expense",
                        now.minusDays(10), "Critical budget transaction");

                // Budget 6: Warning (Credit Card, Shopping)
                Budget warning = new Budget();
                warning.setWallet(wallets.get(2));
                warning.setCategory(expenseCategories.get(5));
                warning.setDescription("Warning Budget");
                warning.setLimitAmount(new BigDecimal("1000000"));
                warning.setCurrentSpending(BigDecimal.ZERO); // 60% spent, ratio ≈ 0.9 / 0.6667 ≈ 1.35
                warning.setStartDate(now.minusDays(20));
                warning.setEndDate(now.plusDays(10));
                budgetRepository.save(warning);
                createTransaction(wallets.get(2), expenseCategories.get(5), new BigDecimal("600000"), "expense",
                        now.minusDays(10), "Warning budget transaction");

                // Budget 7: On Track (Cash Wallet, Transportation)
                Budget onTrack = new Budget();
                onTrack.setWallet(wallets.get(0));
                onTrack.setCategory(expenseCategories.get(1));
                onTrack.setDescription("On Track Budget");
                onTrack.setLimitAmount(new BigDecimal("1000000"));
                onTrack.setCurrentSpending(BigDecimal.ZERO); // 50% spent, ratio ≈ 0.75 / 0.6667 ≈ 1.125
                onTrack.setStartDate(now.minusDays(20));
                onTrack.setEndDate(now.plusDays(10));
                budgetRepository.save(onTrack);
                createTransaction(wallets.get(0), expenseCategories.get(1), new BigDecimal("500000"), "expense",
                        now.minusDays(10), "On track budget transaction");

                // Budget 8: Under Budget (Active) (Bank Account, Entertainment)
                Budget underBudget = new Budget();
                underBudget.setWallet(wallets.get(1));
                underBudget.setCategory(expenseCategories.get(2));
                underBudget.setDescription("Under Budget (Active)");
                underBudget.setLimitAmount(new BigDecimal("1000000"));
                underBudget.setCurrentSpending(BigDecimal.ZERO); // 40% spent, ratio ≈ 0.6 / 0.6667 ≈ 0.9
                underBudget.setStartDate(now.minusDays(20));
                underBudget.setEndDate(now.plusDays(10));
                budgetRepository.save(underBudget);
                createTransaction(wallets.get(1), expenseCategories.get(2), new BigDecimal("400000"), "expense",
                        now.minusDays(10), "Under budget active transaction");

                // Budget 9: Minimal Spending (Credit Card, Housing)
                Budget minimalSpending = new Budget();
                minimalSpending.setWallet(wallets.get(2));
                minimalSpending.setCategory(expenseCategories.get(3));
                minimalSpending.setDescription("Minimal Spending Budget");
                minimalSpending.setLimitAmount(new BigDecimal("1000000"));
                minimalSpending.setCurrentSpending(BigDecimal.ZERO); // 20% spent, ratio ≈ 0.3 / 0.6667 ≈ 0.45
                minimalSpending.setStartDate(now.minusDays(20));
                minimalSpending.setEndDate(now.plusDays(10));
                budgetRepository.save(minimalSpending);
                createTransaction(wallets.get(2), expenseCategories.get(3), new BigDecimal("200000"), "expense",
                        now.minusDays(10), "Minimal spending transaction");
        }

        private void createSavingGoalsForTesting(List<Wallet> wallets, List<Category> categories) {
                LocalDateTime now = LocalDateTime.now();

                // Lấy các category phù hợp cho saving goals (income-related)
                List<Category> incomeCategories = categories.stream()
                        .filter(c -> List.of("Salary", "Freelance", "Gifts", "Investments").contains(c.getName()))
                        .toList();

                // Đảm bảo có đủ categories
                if (incomeCategories.size() < 4) {
                        System.out.println("[DataSeeder] Not enough income categories for saving goals.");
                        return;
                }

                // Saving Goal 1: Not Started (Cash Wallet, Salary)
                SavingGoal notStarted = new SavingGoal();
                notStarted.setWallet(wallets.get(0));
                notStarted.setCategory(incomeCategories.get(0));
                notStarted.setDescription("Not Started Saving Goal");
                notStarted.setTargetAmount(new BigDecimal("1000000"));
                notStarted.setSavedAmount(BigDecimal.ZERO);
                notStarted.setStartDate(now.plusDays(10));
                notStarted.setEndDate(now.plusDays(40));
                savingGoalRepository.save(notStarted);

                // Saving Goal 2: Achieved (Bank Account, Freelance)
                SavingGoal achieved = new SavingGoal();
                achieved.setWallet(wallets.get(1));
                achieved.setCategory(incomeCategories.get(1));
                achieved.setDescription("Achieved Saving Goal");
                achieved.setTargetAmount(new BigDecimal("500000"));
                achieved.setSavedAmount(BigDecimal.ZERO);
                achieved.setStartDate(now.minusDays(60));
                achieved.setEndDate(now.minusDays(30));
                savingGoalRepository.save(achieved);
                createTransaction(wallets.get(1), incomeCategories.get(1), new BigDecimal("500000"), "income",
                        now.minusDays(45), "Achieved saving goal transaction");

                // Saving Goal 3: Partially Achieved (Credit Card, Gifts)
                SavingGoal partiallyAchieved = new SavingGoal();
                partiallyAchieved.setWallet(wallets.get(2));
                partiallyAchieved.setCategory(incomeCategories.get(2));
                partiallyAchieved.setDescription("Partially Achieved Saving Goal");
                partiallyAchieved.setTargetAmount(new BigDecimal("500000"));
                partiallyAchieved.setSavedAmount(BigDecimal.ZERO);
                partiallyAchieved.setStartDate(now.minusDays(60));
                partiallyAchieved.setEndDate(now.minusDays(30));
                savingGoalRepository.save(partiallyAchieved);
                createTransaction(wallets.get(2), incomeCategories.get(2), new BigDecimal("400000"), "income",
                        now.minusDays(45), "Partially achieved saving goal transaction");

                // Saving Goal 4: Missed Target (Cash Wallet, Investments)
                SavingGoal missedTarget = new SavingGoal();
                missedTarget.setWallet(wallets.get(0));
                missedTarget.setCategory(incomeCategories.get(3));
                missedTarget.setDescription("Missed Target Saving Goal");
                missedTarget.setTargetAmount(new BigDecimal("500000"));
                missedTarget.setSavedAmount(BigDecimal.ZERO);
                missedTarget.setStartDate(now.minusDays(60));
                missedTarget.setEndDate(now.minusDays(30));
                savingGoalRepository.save(missedTarget);
                createTransaction(wallets.get(0), incomeCategories.get(3), new BigDecimal("300000"), "income",
                        now.minusDays(45), "Missed target saving goal transaction");

                // Saving Goal 5: Achieved Early (Bank Account, Salary)
                SavingGoal achievedEarly = new SavingGoal();
                achievedEarly.setWallet(wallets.get(1));
                achievedEarly.setCategory(incomeCategories.get(0));
                achievedEarly.setDescription("Achieved Early Saving Goal");
                achievedEarly.setTargetAmount(new BigDecimal("500000"));
                achievedEarly.setSavedAmount(BigDecimal.ZERO);
                achievedEarly.setStartDate(now.minusDays(20));
                achievedEarly.setEndDate(now.plusDays(10));
                savingGoalRepository.save(achievedEarly);
                createTransaction(wallets.get(1), incomeCategories.get(0), new BigDecimal("500000"), "income",
                        now.minusDays(10), "Achieved early saving goal transaction");

                // Saving Goal 6: Ahead (Credit Card, Freelance)
                SavingGoal ahead = new SavingGoal();
                ahead.setWallet(wallets.get(2));
                ahead.setCategory(incomeCategories.get(1));
                ahead.setDescription("Ahead Saving Goal");
                ahead.setTargetAmount(new BigDecimal("850000"));
                ahead.setSavedAmount(BigDecimal.ZERO); // 80% saved, ratio ≈ 0.8 / 0.6667 ≈ 1.2
                ahead.setStartDate(now.minusDays(20));
                ahead.setEndDate(now.plusDays(10));
                savingGoalRepository.save(ahead);
                createTransaction(wallets.get(2), incomeCategories.get(1), new BigDecimal("800000"), "income",
                        now.minusDays(10), "Ahead saving goal transaction");

                // Saving Goal 7: On Track (Cash Wallet, Gifts)
                SavingGoal onTrack = new SavingGoal();
                onTrack.setWallet(wallets.get(0));
                onTrack.setCategory(incomeCategories.get(2));
                onTrack.setDescription("On Track Saving Goal");
                onTrack.setTargetAmount(new BigDecimal("1000000"));
                onTrack.setSavedAmount(BigDecimal.ZERO); // 60% saved, ratio ≈ 0.6 / 0.6667 ≈ 0.9
                onTrack.setStartDate(now.minusDays(20));
                onTrack.setEndDate(now.plusDays(10));
                savingGoalRepository.save(onTrack);
                createTransaction(wallets.get(0), incomeCategories.get(2), new BigDecimal("600000"), "income",
                        now.minusDays(10), "On track saving goal transaction");

                // Saving Goal 8: Slightly Behind (Bank Account, Investments)
                SavingGoal slightlyBehind = new SavingGoal();
                slightlyBehind.setWallet(wallets.get(1));
                slightlyBehind.setCategory(incomeCategories.get(3));
                slightlyBehind.setDescription("Slightly Behind Saving Goal");
                slightlyBehind.setTargetAmount(new BigDecimal("1000000"));
                slightlyBehind.setSavedAmount(BigDecimal.ZERO); // 50% saved, ratio ≈ 0.5 / 0.6667 ≈ 0.75
                slightlyBehind.setStartDate(now.minusDays(20));
                slightlyBehind.setEndDate(now.plusDays(10));
                savingGoalRepository.save(slightlyBehind);
                createTransaction(wallets.get(1), incomeCategories.get(3), new BigDecimal("500000"), "income",
                        now.minusDays(10), "Slightly behind saving goal transaction");

                // Saving Goal 9: At Risk (Credit Card, Salary)
                SavingGoal atRisk = new SavingGoal();
                atRisk.setWallet(wallets.get(2));
                atRisk.setCategory(incomeCategories.get(0));
                atRisk.setDescription("At Risk Saving Goal");
                atRisk.setTargetAmount(new BigDecimal("1000000"));
                atRisk.setSavedAmount(BigDecimal.ZERO); // 30% saved, ratio ≈ 0.3 / 0.6667 ≈ 0.45
                atRisk.setStartDate(now.minusDays(20));
                atRisk.setEndDate(now.plusDays(10));
                savingGoalRepository.save(atRisk);
                createTransaction(wallets.get(2), incomeCategories.get(0), new BigDecimal("300000"), "income",
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

                        // Update wallet balance using repository method
                        BigDecimal balanceChange = type.equals("expense") ? amount.negate() : amount;
                        walletRepository.updateBalance(wallet.getWalletId(), balanceChange);

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