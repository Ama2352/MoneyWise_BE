package JavaProject.MoneyManagement_BE_SE330.services.impls;

import JavaProject.MoneyManagement_BE_SE330.helper.ApplicationMapper;
import JavaProject.MoneyManagement_BE_SE330.helper.HelperFunctions;
import JavaProject.MoneyManagement_BE_SE330.helper.ResourceNotFoundException;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.transaction.*;
import JavaProject.MoneyManagement_BE_SE330.models.entities.*;
import JavaProject.MoneyManagement_BE_SE330.repositories.*;
import JavaProject.MoneyManagement_BE_SE330.services.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final WalletRepository walletRepository;
    private final ApplicationMapper applicationMapper;
    private final UserRepository userRepository;
    private final BudgetRepository budgetRepository;
    private final SavingGoalRepository savingGoalRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TransactionDTO> getAllTransactions() {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        return transactionRepository.findAllByWalletUser(currentUser)
                .stream()
                .map(applicationMapper::toTransactionDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionDTO> getTransactionsByWalletId(UUID walletId) {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        if (!wallet.getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not have access to this wallet's transactions");
        }
        return transactionRepository.findByWalletWalletId(walletId)
                .stream()
                .map(applicationMapper::toTransactionDTO)
                .toList();
    }


    @Override
    @Transactional(readOnly = true)
    public TransactionDTO getTransactionById(UUID transactionId) {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (!transaction.getWallet().getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not have access to this transaction");
        }

        return applicationMapper.toTransactionDTO(transaction);
    }

    @Override
    @Transactional
    public TransactionDTO createTransaction(CreateTransactionDTO model) {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);

        Wallet wallet = walletRepository.findById(model.getWalletID())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        if(!wallet.getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not own this wallet");
        }

        Category category = categoryRepository.findById(model.getCategoryID())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if(!category.getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not own this category");
        }

        Transaction transaction = applicationMapper.toTransactionEntity(model);
        transaction.setWallet(wallet);
        transaction.setCategory(category);

        transactionRepository.save(transaction);

        // Update UserBudget for Expense transactions
        if (Objects.equals(transaction.getType(), "expense")) {
            List<Budget> budgets = budgetRepository.findByCategoryAndWalletAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                    transaction.getCategory(),
                    transaction.getWallet(),
                    transaction.getTransactionDate(),
                    transaction.getTransactionDate()
            );
            if (!budgets.isEmpty()) {
                budgetRepository.updateCurrentSpending(
                        transaction.getCategory(),
                        transaction.getWallet(),
                        transaction.getAmount(),
                        transaction.getTransactionDate()
                );
            }
        }

        // Update UserSavingGoal for Income transactions
        if (Objects.equals(transaction.getType(), "income")) {
            List<SavingGoal> goals = savingGoalRepository.findByCategoryAndWalletAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                    transaction.getCategory(),
                    transaction.getWallet(),
                    transaction.getTransactionDate(),
                    transaction.getTransactionDate()
            );
            if (!goals.isEmpty()) {
                savingGoalRepository.updateSavedAmount(
                        transaction.getCategory(),
                        transaction.getWallet(),
                        transaction.getAmount(),
                        transaction.getTransactionDate()
                );
            }
        }

        // Update Wallet Balance
        BigDecimal balanceChange = Objects.equals(transaction.getType(), "income")
                ? transaction.getAmount()
                : transaction.getAmount().negate();
        walletRepository.updateBalance(transaction.getWallet().getWalletId(), balanceChange);

        return applicationMapper.toTransactionDTO(transaction);
    }

    @Override
    @Transactional
    public TransactionDTO updateTransaction(UpdateTransactionDTO model) {
        // Fetch existing transaction
        Transaction transaction = transactionRepository.findById(model.getTransactionID())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        if (!transaction.getWallet().getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not have access to update this transaction");
        }

        // Fetch Wallet and Category for the updated values
        Wallet wallet = walletRepository.findById(model.getWalletID())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        if (!wallet.getUser().equals(currentUser)) {
            throw new AccessDeniedException("You cannot move transaction to a wallet you don't own");
        }

        Category category = categoryRepository.findById(model.getCategoryID())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (!category.getUser().equals(currentUser)) {
            throw new AccessDeniedException("You cannot assign a category you don't own");
        }

        // Check if relevant fields have changed
        boolean hasChanges = !Objects.equals(transaction.getAmount(), model.getAmount()) ||
                !Objects.equals(transaction.getCategory().getCategoryId(), model.getCategoryID()) ||
                !Objects.equals(transaction.getWallet().getWalletId(), model.getWalletID()) ||
                !Objects.equals(transaction.getTransactionDate(), model.getTransactionDate());

        // Step 1: Reverse original effects if relevant fields changed
        if (hasChanges) {
            if (Objects.equals(transaction.getType(), "expense")) {
                List<Budget> budgets = budgetRepository.findByCategoryAndWalletAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        transaction.getCategory(),
                        transaction.getWallet(),
                        transaction.getTransactionDate(),
                        transaction.getTransactionDate()
                );
                if (!budgets.isEmpty()) {
                    budgetRepository.updateCurrentSpending(
                            transaction.getCategory(),
                            transaction.getWallet(),
                            transaction.getAmount().negate(), // Reverse the amount
                            transaction.getTransactionDate()
                    );
                }
            } else if (Objects.equals(transaction.getType(), "income")) {
                List<SavingGoal> goals = savingGoalRepository.findByCategoryAndWalletAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        transaction.getCategory(),
                        transaction.getWallet(),
                        transaction.getTransactionDate(),
                        transaction.getTransactionDate()
                );
                if (!goals.isEmpty()) {
                    savingGoalRepository.updateSavedAmount(
                            transaction.getCategory(),
                            transaction.getWallet(),
                            transaction.getAmount().negate(), // Reverse the amount
                            transaction.getTransactionDate()
                    );
                }
            }

            // Reverse Wallet Balance
            BigDecimal originalBalanceChange = Objects.equals(transaction.getType(), "income")
                    ? transaction.getAmount().negate() // Undo income
                    : transaction.getAmount(); // Undo expense
            walletRepository.updateBalance(transaction.getWallet().getWalletId(), originalBalanceChange);
        }

        // Step 2: Update transaction fields (excluding Type)
        transaction.setAmount(model.getAmount());
        transaction.setDescription(model.getDescription());
        transaction.setTransactionDate(model.getTransactionDate());
        transaction.setWallet(wallet);
        transaction.setCategory(category);

        // Save updated transaction
        Transaction updatedTransaction = transactionRepository.save(transaction);

        // Step 3: Apply new effects if relevant fields changed
        if (hasChanges) {
            BigDecimal newBalanceChange;
            if (Objects.equals(transaction.getType(), "expense")) {
                List<Budget> budgets = budgetRepository.findByCategoryAndWalletAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        transaction.getCategory(),
                        transaction.getWallet(),
                        transaction.getTransactionDate(),
                        transaction.getTransactionDate()
                );
                if (!budgets.isEmpty()) {
                    budgetRepository.updateCurrentSpending(
                            transaction.getCategory(),
                            transaction.getWallet(),
                            transaction.getAmount(),
                            transaction.getTransactionDate()
                    );
                }
                newBalanceChange = transaction.getAmount().negate(); // Decrease balance
            } else {
                List<SavingGoal> goals = savingGoalRepository.findByCategoryAndWalletAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        transaction.getCategory(),
                        transaction.getWallet(),
                        transaction.getTransactionDate(),
                        transaction.getTransactionDate()
                );
                if (!goals.isEmpty()) {
                    savingGoalRepository.updateSavedAmount(
                            transaction.getCategory(),
                            transaction.getWallet(),
                            transaction.getAmount(),
                            transaction.getTransactionDate()
                    );
                }
                newBalanceChange = transaction.getAmount(); // Increase balance
            }

            // Update Wallet Balance
            walletRepository.updateBalance(transaction.getWallet().getWalletId(), newBalanceChange);
        }

        return applicationMapper.toTransactionDTO(updatedTransaction);
    }

    @Override
    @Transactional
    public UUID deleteTransactionById(UUID transactionId) {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (!transaction.getWallet().getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not have access to delete this transaction");
        }

        // Reverse effects on Budget or SavingGoal
        if (Objects.equals(transaction.getType(), "expense")) {
            List<Budget> budgets = budgetRepository.findByCategoryAndWalletAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                    transaction.getCategory(),
                    transaction.getWallet(),
                    transaction.getTransactionDate(),
                    transaction.getTransactionDate()
            );
            if (!budgets.isEmpty()) {
                budgetRepository.updateCurrentSpending(
                        transaction.getCategory(),
                        transaction.getWallet(),
                        transaction.getAmount().negate(),
                        transaction.getTransactionDate()
                );
            }
        } else if (Objects.equals(transaction.getType(), "income")) {
            List<SavingGoal> goals = savingGoalRepository.findByCategoryAndWalletAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                    transaction.getCategory(),
                    transaction.getWallet(),
                    transaction.getTransactionDate(),
                    transaction.getTransactionDate()
            );
            if (!goals.isEmpty()) {
                savingGoalRepository.updateSavedAmount(
                        transaction.getCategory(),
                        transaction.getWallet(),
                        transaction.getAmount().negate(),
                        transaction.getTransactionDate()
                );
            }
        }

        // Reverse Wallet Balance
        BigDecimal balanceChange = Objects.equals(transaction.getType(), "income")
                ? transaction.getAmount().negate() // Undo income
                : transaction.getAmount(); // Undo expense
        walletRepository.updateBalance(transaction.getWallet().getWalletId(), balanceChange);

        // Delete transaction
        transactionRepository.delete(transaction);
        return transactionId;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionDetailDTO> getTransactionsByDateRange(GetTransactionsByDateRangeDTO filter) {
        log.info("Fetching transactions from {} to {}", filter.getStartDate(), filter.getEndDate());

        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        List<Transaction> allTransactions = transactionRepository.findAllByWalletUser(currentUser);

        // Convert LocalDate to LocalDateTime
        LocalDateTime startDateTime = filter.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = filter.getEndDate().atTime(LocalTime.MAX); // includes the full day

        List<Transaction> filteredTransactions = allTransactions.stream()
                .filter(t -> !t.getTransactionDate().isBefore(startDateTime)
                        && !t.getTransactionDate().isAfter(endDateTime))
                .filter(t -> filter.getType() == null ||
                        t.getType().equalsIgnoreCase(filter.getType()))
                .filter(t -> filter.getCategory() == null ||
                        t.getCategory().getName().equalsIgnoreCase(filter.getCategory()))
                .filter(t -> {
                    if (filter.getTimeRange() == null || !filter.getTimeRange().contains("-")) return true;
                    try {
                        String[] parts = filter.getTimeRange().split("-");
                        LocalTime start = LocalTime.parse(parts[0].trim());
                        LocalTime end = LocalTime.parse(parts[1].trim());
                        LocalTime txnTime = t.getTransactionDate().toLocalTime();
                        return !txnTime.isBefore(start) && !txnTime.isAfter(end);
                    } catch (Exception e) {
                        return true; // fallback if parsing fails
                    }
                })
                .filter(t -> {
                    if (filter.getDayOfWeek() == null) return true;
                    try {
                        DayOfWeek expectedDay = DayOfWeek.valueOf(filter.getDayOfWeek().toUpperCase());
                        return t.getTransactionDate().getDayOfWeek() == expectedDay;
                    } catch (Exception e) {
                        return true;
                    }
                })
                .sorted(Comparator.comparing(Transaction::getTransactionDate).reversed())
                .toList();

        return filteredTransactions.stream()
                .map(applicationMapper::toTransactionDetailDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionDetailDTO> searchTransactions(SearchTransactionsDTO filter) {
        log.info("Searching transactions with filters from {} to {}", filter.getStartDate(), filter.getEndDate());

        User currentUser = HelperFunctions.getCurrentUser(userRepository);

        List<UUID> userWalletIds = walletRepository.findAllByUser(currentUser)
                .stream()
                .map(Wallet::getWalletId)
                .toList();

        LocalDateTime startDateTime = filter.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = filter.getEndDate().atTime(LocalTime.MAX);

        List<Transaction> transactions = transactionRepository.findAllByWalletUser(currentUser)
                .stream()
                .filter(t -> !t.getTransactionDate().isBefore(startDateTime) && !t.getTransactionDate().isAfter(endDateTime))
                .filter(t -> filter.getType() == null ||
                        t.getType().equalsIgnoreCase(filter.getType()))
                .filter(t -> filter.getCategory() == null ||
                        t.getCategory().getName().equalsIgnoreCase(filter.getCategory()))
                .filter(t -> {
                    if (filter.getAmountRange() == null || !filter.getAmountRange().contains("-")) return true;
                    try {
                        String[] parts = filter.getAmountRange().split("-");
                        BigDecimal min = new BigDecimal(parts[0]);
                        BigDecimal max = new BigDecimal(parts[1]);
                        BigDecimal amount = t.getAmount().abs();
                        return amount.compareTo(min) >= 0 && amount.compareTo(max) <= 0;
                    } catch (Exception e) {
                        return true;
                    }
                })
                .filter(t -> filter.getKeywords() == null ||
                        (t.getDescription() != null && t.getDescription().toLowerCase().contains(filter.getKeywords().toLowerCase())))
                .filter(t -> {
                    if (filter.getTimeRange() == null || !filter.getTimeRange().contains("-")) return true;
                    try {
                        String[] times = filter.getTimeRange().split("-");
                        LocalTime start = LocalTime.parse(times[0]);
                        LocalTime end = LocalTime.parse(times[1]);
                        LocalTime txnTime = t.getTransactionDate().toLocalTime();
                        return !txnTime.isBefore(start) && !txnTime.isAfter(end);
                    } catch (Exception e) {
                        return true;
                    }
                })
                .filter(t -> {
                    if (filter.getDayOfWeek() == null) return true;
                    try {
                        DayOfWeek expected = DayOfWeek.valueOf(filter.getDayOfWeek().toUpperCase());
                        return t.getTransactionDate().getDayOfWeek() == expected;
                    } catch (Exception e) {
                        return true;
                    }
                })
                .sorted(Comparator.comparing(Transaction::getTransactionDate).reversed())
                .toList();

        return transactions.stream()
                .map(applicationMapper::toTransactionDetailDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryBreakdownDTO> getCategoryBreakdown(LocalDate startDate, LocalDate endDate) {
        try {
            log.info("Generating category breakdown for period {} to {}", startDate, endDate);

            User currentUser = HelperFunctions.getCurrentUser(userRepository); // Your method to get authenticated user

            // Get all wallets owned by the current user
            List<Wallet> userWallets = walletRepository.findAllByUser(currentUser);
            Set<UUID> userWalletIds = userWallets.stream()
                    .map(Wallet::getWalletId)
                    .collect(Collectors.toSet());

            // Fetch transactions filtered by date and wallet ownership
            List<Transaction> transactions = transactionRepository.findAllByWalletUser(currentUser).stream()
                    .filter(t -> !t.getTransactionDate().toLocalDate().isBefore(startDate) &&
                            !t.getTransactionDate().toLocalDate().isAfter(endDate) &&
                            userWalletIds.contains(t.getWallet().getWalletId()))
                    .toList();

            // Separate income and expenses
            List<Transaction> incomeTransactions = transactions.stream()
                    .filter(t -> "income".equalsIgnoreCase(t.getType()))
                    .toList();

            List<Transaction> expenseTransactions = transactions.stream()
                    .filter(t -> "expense".equalsIgnoreCase(t.getType()))
                    .toList();


            BigDecimal totalIncome = incomeTransactions.stream()
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalExpense = expenseTransactions.stream()
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .abs();

            // Group by category name and calculate totals and percentages
            Map<String, List<Transaction>> groupedByCategory = transactions.stream()
                    .collect(Collectors.groupingBy(t -> t.getCategory().getName()));

            return groupedByCategory.entrySet().stream()
                    .map(entry -> {
                        String categoryName = entry.getKey();
                        List<Transaction> categoryTransactions = entry.getValue();
                        Category category = categoryTransactions.getFirst().getCategory();

                        BigDecimal categoryIncome = categoryTransactions.stream()
                                .map(Transaction::getAmount)
                                .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        BigDecimal categoryExpense = categoryTransactions.stream()
                                .map(Transaction::getAmount)
                                .filter(amount -> amount.compareTo(BigDecimal.ZERO) < 0)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .abs();

                        BigDecimal incomePercentage = totalIncome.compareTo(BigDecimal.ZERO) == 0
                                ? BigDecimal.ZERO
                                : categoryIncome.divide(totalIncome, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .setScale(2, RoundingMode.HALF_UP);

                        BigDecimal expensePercentage = totalExpense.compareTo(BigDecimal.ZERO) == 0
                                ? BigDecimal.ZERO
                                : categoryExpense.divide(totalExpense, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .setScale(2, RoundingMode.HALF_UP);

                        // Fetch Budget and SavingGoal
                        List<Budget> budgets = budgetRepository.findByCategoryAndWalletUserAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                                category, currentUser, endDate.atStartOfDay(), startDate.atStartOfDay());
                        List<SavingGoal> goals = savingGoalRepository.findByCategoryAndWalletUserAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                                category, currentUser, endDate.atStartOfDay(), startDate.atStartOfDay());

                        BigDecimal budgetLimit = budgets.isEmpty() ? BigDecimal.ZERO : budgets.getFirst().getLimitAmount();
                        BigDecimal budgetSpending = budgets.isEmpty() ? BigDecimal.ZERO : budgets.getFirst().getCurrentSpending();
                        BigDecimal goalTarget = goals.isEmpty() ? BigDecimal.ZERO : goals.getFirst().getTargetAmount();
                        BigDecimal goalSaved = goals.isEmpty() ? BigDecimal.ZERO : goals.getFirst().getSavedAmount();

                        return new CategoryBreakdownDTO(
                                categoryName,
                                categoryIncome,
                                categoryExpense,
                                incomePercentage,
                                expensePercentage,
                                budgetLimit,
                                budgetSpending,
                                goalTarget,
                                goalSaved
                        );
                    })
                    .sorted(Comparator.comparing(dto -> dto.getTotalIncome().add(dto.getTotalExpense()), Comparator.reverseOrder()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error generating category breakdown", e);
            throw new RuntimeException("Failed to generate category breakdown", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DailySummaryDTO getDailySummary(LocalDate date) {
        // 1) resolve user and their wallets
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        List<UUID> walletIds = walletRepository
                .findAllByUser(currentUser)
                .stream()
                .map(Wallet::getWalletId)
                .toList();

        // 2) define day‐bounds
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay   = date.atTime(LocalTime.MAX);

        // 3) fetch daily transactions (for totals)
        List<Transaction> dailyTxs = transactionRepository
                .findByWalletWalletIdInAndTransactionDateBetween(
                        walletIds, startOfDay, endOfDay);

        // 4) compute totalIncome & totalExpenses by type
        BigDecimal totalIncome = dailyTxs.stream()
                .filter(tx -> "income".equalsIgnoreCase(tx.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = dailyTxs.stream()
                .filter(tx -> "expense".equalsIgnoreCase(tx.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5) define current week (Monday → next Monday)
        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd   = weekStart.plusDays(7);
        LocalDateTime weekStartDT = weekStart.atStartOfDay();
        LocalDateTime weekEndDT   = weekEnd.atStartOfDay();

        // 6) fetch week transactions (for dailyDetails)
        List<Transaction> weekTxs = transactionRepository
                .findByWalletWalletIdInAndTransactionDateBetween(
                        walletIds, weekStartDT, weekEndDT);

        // 7) group by DayOfWeek
        Map<DayOfWeek, List<Transaction>> txsByDow = weekTxs.stream()
                .collect(Collectors.groupingBy(tx -> tx.getTransactionDate().getDayOfWeek()));

        // 8) build a full‐week list, filtering by type
        List<DailyDetailDTO> dailyDetails = Arrays.stream(DayOfWeek.values())
                .map(dow -> {
                    List<Transaction> txs = txsByDow.getOrDefault(dow, List.of());

                    BigDecimal income = txs.stream()
                            .filter(tx -> "income".equalsIgnoreCase(tx.getType()))
                            .map(Transaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal expense = txs.stream()
                            .filter(tx -> "expense".equalsIgnoreCase(tx.getType()))
                            .map(Transaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return new DailyDetailDTO(
                            dow.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                            income,
                            expense
                    );
                })
                .collect(Collectors.toList());

        // 9) assemble and return
        DailySummaryDTO summary = new DailySummaryDTO();
        summary.setDailyDetails(dailyDetails);
        summary.setTotalIncome(totalIncome);
        summary.setTotalExpenses(totalExpenses);
        return summary;
    }

    @Override
    @Transactional(readOnly = true)
    public WeeklySummaryDTO getWeeklySummary(LocalDate weekStartDate) {
        // 1. Get all wallet IDs for the user
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        List<UUID> userWalletIds = walletRepository.findAllByUser(currentUser)
                .stream().map(Wallet::getWalletId).collect(Collectors.toList());

        if (userWalletIds.isEmpty()) {
            // No wallets -> empty summary
            WeeklySummaryDTO empty = new WeeklySummaryDTO();
            empty.setWeeklyDetails(new ArrayList<>());
            empty.setTotalIncome(BigDecimal.ZERO);
            empty.setTotalExpenses(BigDecimal.ZERO);
            return empty;
        }

        // 2. Determine the range for the entire month that contains the weekStartDate
        LocalDate firstDayOfMonth = weekStartDate.withDayOfMonth(1);
        LocalDate lastDayOfMonth = firstDayOfMonth.plusMonths(1).minusDays(1);

        // 3. Expand to full weeks
        LocalDate firstWeekStart = firstDayOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate lastWeekEnd = lastDayOfMonth.with(TemporalAdjusters.next(DayOfWeek.SUNDAY)).plusDays(1);

        // 4. Fetch all transactions in the expanded range
        List<Transaction> allMonthTransactions = transactionRepository
                .findByWalletWalletIdInAndTransactionDateBetween(
                        userWalletIds,
                        firstWeekStart.atStartOfDay(),
                        lastWeekEnd.atStartOfDay()
                );

        // 5. Process weekly details
        List<WeeklyDetailDTO> weeklyDetails = new ArrayList<>();
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        int weekNumber = 1;
        for (LocalDate weekStart = firstWeekStart; weekStart.isBefore(lastWeekEnd); weekStart = weekStart.plusWeeks(1), weekNumber++) {
            LocalDate weekEnd = weekStart.plusDays(7);

            LocalDate finalWeekStart = weekStart;
            List<Transaction> weekTxs = allMonthTransactions.stream()
                    .filter(t -> {
                        LocalDate date = t.getTransactionDate().toLocalDate();
                        return !date.isBefore(finalWeekStart) && date.isBefore(weekEnd);
                    })
                    .toList();

            BigDecimal income = weekTxs.stream()
                    .filter(t -> "income".equalsIgnoreCase(t.getType()))
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal expense = weekTxs.stream()
                    .filter(t -> "expense".equalsIgnoreCase(t.getType()))
                    .map(t -> t.getAmount().abs())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalIncome = totalIncome.add(income);
            totalExpenses = totalExpenses.add(expense);

            WeeklyDetailDTO detail = new WeeklyDetailDTO();
            detail.setWeekNumber(String.valueOf(weekNumber));
            detail.setIncome(income);
            detail.setExpense(expense);
            weeklyDetails.add(detail);
        }

        WeeklySummaryDTO result = new WeeklySummaryDTO();
        result.setWeeklyDetails(weeklyDetails);
        result.setTotalIncome(totalIncome);
        result.setTotalExpenses(totalExpenses);

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlySummaryDTO getMonthlySummary(YearMonth yearMonth) {
        // 1. Get wallet IDs for the user
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        List<UUID> userWalletIds = walletRepository.findAllByUser(currentUser)
                .stream().map(Wallet::getWalletId).collect(Collectors.toList());

        if (userWalletIds.isEmpty()) {
            MonthlySummaryDTO empty = new MonthlySummaryDTO();
            empty.setMonthlyDetails(new ArrayList<>());
            empty.setTotalIncome(BigDecimal.ZERO);
            empty.setTotalExpenses(BigDecimal.ZERO);
            return empty;
        }

        // 2. Define year range
        LocalDate startOfYear = yearMonth.atDay(1).withDayOfYear(1);
        LocalDate endOfYear = startOfYear.plusYears(1).minusDays(1);

        // 3. Fetch all transactions for the year for the user's wallets
        List<Transaction> yearTransactions = transactionRepository
                .findByWalletWalletIdInAndTransactionDateBetween(
                        userWalletIds,
                        startOfYear.atStartOfDay(),
                        endOfYear.atTime(LocalTime.MAX)
                );

        // 4. Build MonthlyDetailDTOs for each month
        List<MonthlyDetailDTO> monthlyDetails = IntStream.rangeClosed(1, 12)
                .mapToObj(month -> {
                    List<Transaction> monthlyTxs = yearTransactions.stream()
                            .filter(t -> t.getTransactionDate().getMonthValue() == month)
                            .toList();

                    BigDecimal income = monthlyTxs.stream()
                            .filter(t -> "income".equalsIgnoreCase(t.getType()))
                            .map(Transaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal expense = monthlyTxs.stream()
                            .filter(t -> "expense".equalsIgnoreCase(t.getType()))
                            .map(t -> t.getAmount().abs())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    MonthlyDetailDTO dto = new MonthlyDetailDTO();
                    dto.setMonthName(Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault()));
                    dto.setIncome(income);
                    dto.setExpense(expense);
                    return dto;
                })
                .collect(Collectors.toList());

        // 5. Sum total income and expenses for requested month
        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);

        BigDecimal totalIncome = yearTransactions.stream()
                .filter(t -> {
                    LocalDate d = t.getTransactionDate().toLocalDate();
                    return !d.isBefore(startOfMonth) && !d.isAfter(endOfMonth);
                })
                .filter(t -> "income".equalsIgnoreCase(t.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = yearTransactions.stream()
                .filter(t -> {
                    LocalDate d = t.getTransactionDate().toLocalDate();
                    return !d.isBefore(startOfMonth) && !d.isAfter(endOfMonth);
                })
                .filter(t -> "expense".equalsIgnoreCase(t.getType()))
                .map(t -> t.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 6. Return DTO
        MonthlySummaryDTO result = new MonthlySummaryDTO();
        result.setMonthlyDetails(monthlyDetails);
        result.setTotalIncome(totalIncome);
        result.setTotalExpenses(totalExpenses);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public YearlySummaryDTO getYearlySummary(int year) {
        // 1. Get all wallet UUIDs for the current user
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        List<UUID> userWalletIds = walletRepository.findAllByUser(currentUser)
                .stream().map(Wallet::getWalletId).collect(Collectors.toList());

        // 2. Define the year range (last 5 years up to the specified year)
        int yearsToShow = 5;
        int startYear = year - yearsToShow + 1;

        LocalDateTime startOfRange = LocalDateTime.of(startYear, 1, 1, 0, 0);
        LocalDateTime endOfRange = LocalDateTime.of(year, 12, 31, 23, 59, 59, 999_000_000);

        // 3. Fetch transactions across that range for user's wallets
        List<Transaction> allTransactions = transactionRepository
                .findByWalletWalletIdInAndTransactionDateBetween(userWalletIds, startOfRange, endOfRange);

        // 4. Build YearlyDetailDTO list
        List<YearlyDetailDTO> yearlyDetails = IntStream.rangeClosed(startYear, year)
                .mapToObj(yr -> {
                    List<Transaction> yearTxs = allTransactions.stream()
                            .filter(t -> t.getTransactionDate().getYear() == yr)
                            .toList();

                    BigDecimal income = yearTxs.stream()
                            .filter(t -> "income".equalsIgnoreCase(t.getType()))
                            .map(Transaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal expense = yearTxs.stream()
                            .filter(t -> "expense".equalsIgnoreCase(t.getType()))
                            .map(Transaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add).abs();

                    YearlyDetailDTO detail = new YearlyDetailDTO();
                    detail.setYear(String.valueOf(yr));
                    detail.setIncome(income);
                    detail.setExpense(expense);
                    return detail;
                })
                .collect(Collectors.toList());

        // 5. Compute total income and expense for the requested year
        List<Transaction> currentYearTxs = allTransactions.stream()
                .filter(t -> t.getTransactionDate().getYear() == year)
                .toList();

        BigDecimal totalIncome = currentYearTxs.stream()
                .filter(t -> "income".equalsIgnoreCase(t.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = currentYearTxs.stream()
                .filter(t -> "expense".equalsIgnoreCase(t.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add).abs();

        // 6. Create and return summary DTO
        YearlySummaryDTO result = new YearlySummaryDTO();
        result.setYearlyDetails(yearlyDetails);
        result.setTotalIncome(totalIncome);
        result.setTotalExpenses(totalExpenses);
        return result;
    }
}
