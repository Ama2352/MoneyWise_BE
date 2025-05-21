package JavaProject.MoneyManagement_BE_SE330.services.impls;

import JavaProject.MoneyManagement_BE_SE330.helper.ApplicationMapper;
import JavaProject.MoneyManagement_BE_SE330.helper.HelperFunctions;
import JavaProject.MoneyManagement_BE_SE330.helper.ResourceNotFoundException;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.transaction.*;
import JavaProject.MoneyManagement_BE_SE330.models.entities.Category;
import JavaProject.MoneyManagement_BE_SE330.models.entities.Transaction;
import JavaProject.MoneyManagement_BE_SE330.models.entities.User;
import JavaProject.MoneyManagement_BE_SE330.models.entities.Wallet;
import JavaProject.MoneyManagement_BE_SE330.repositories.CategoryRepository;
import JavaProject.MoneyManagement_BE_SE330.repositories.TransactionRepository;
import JavaProject.MoneyManagement_BE_SE330.repositories.UserRepository;
import JavaProject.MoneyManagement_BE_SE330.repositories.WalletRepository;
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
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final WalletRepository walletRepository;
    private final ApplicationMapper applicationMapper;
    private final UserRepository userRepository;

    @Override
    public List<TransactionDTO> getAllTransactions() {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        return transactionRepository.findAllByWalletUser(currentUser)
                .stream()
                .map(applicationMapper::toTransactionDTO)
                .toList();
    }

    @Override
    public List<TransactionDTO> getTransactionsByWalletId(UUID walletId) {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        if (!wallet.getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not have access to this wallet's transactions");
        }
        return transactionRepository.findByWalletWalletID(walletId)
                .stream()
                .map(applicationMapper::toTransactionDTO)
                .toList();
    }

    @Override
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
        return applicationMapper.toTransactionDTO(transaction);
    }

    @Override
    public TransactionDTO updateTransaction(UpdateTransactionDTO model) {
        var transaction = transactionRepository.findById(model.getTransactionID())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        // Update fields - assuming UpdateTransactionDTO has the fields
        transaction.setAmount(model.getAmount());
        transaction.setDescription(model.getDescription());
        transaction.setTransactionDate(model.getTransactionDate());
        transaction.setType(model.getType());

        // Fetch Wallet entity by ID
        Wallet wallet = walletRepository.findById(model.getWalletID())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        // Fetch Category entity by ID
        Category category = categoryRepository.findById(model.getCategoryID())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        User currentUser = HelperFunctions.getCurrentUser(userRepository);

        if(!transaction.getWallet().getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not have access to update this transaction");
        }

        if(!wallet.getUser().equals(currentUser)) {
            throw new AccessDeniedException("You cannot move transaction to a wallet you don't own");
        }

        if (!category.getUser().equals(currentUser)) {
            throw new AccessDeniedException("You cannot assign a category you don't own");
        }

        // Set the references on your Transaction entity
        transaction.setWallet(wallet);
        transaction.setCategory(category);

        transactionRepository.save(transaction);
        return applicationMapper.toTransactionDTO(transaction);
    }

    @Override
    public UUID deleteTransactionById(UUID transactionId) {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if(!transaction.getWallet().getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not have access to delete this transaction");
        }

        transactionRepository.delete(transaction);
        return transactionId;
    }

    @Override
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
                .filter(t -> {
                    String type = filter.getType();
                    if (type == null) return true;
                    return type.equalsIgnoreCase("expense") ? t.getAmount().compareTo(BigDecimal.ZERO) < 0
                            : !type.equalsIgnoreCase("income") || t.getAmount().compareTo(BigDecimal.ZERO) > 0;
                })
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
    public List<TransactionDetailDTO> searchTransactions(SearchTransactionsDTO filter) {
        log.info("Searching transactions with filters from {} to {}", filter.getStartDate(), filter.getEndDate());

        User currentUser = HelperFunctions.getCurrentUser(userRepository);

        List<UUID> userWalletIds = walletRepository.findAllByUser(currentUser)
                .stream()
                .map(Wallet::getWalletID)
                .toList();

        LocalDateTime startDateTime = filter.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = filter.getEndDate().atTime(LocalTime.MAX);

        List<Transaction> transactions = transactionRepository.findAllByWalletUser(currentUser)
                .stream()
                .filter(t -> !t.getTransactionDate().isBefore(startDateTime) && !t.getTransactionDate().isAfter(endDateTime))
                .filter(t -> {
                    if (filter.getType() == null) return true;
                    return filter.getType().equalsIgnoreCase("expense") ? t.getAmount().compareTo(BigDecimal.ZERO) < 0 :
                            filter.getType().equalsIgnoreCase("income") && t.getAmount().compareTo(BigDecimal.ZERO) > 0;
                })
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
    public List<CategoryBreakdownDTO> getCategoryBreakdown(LocalDate startDate, LocalDate endDate) {
        try {
            log.info("Generating category breakdown for period {} to {}", startDate, endDate);

            User currentUser = HelperFunctions.getCurrentUser(userRepository); // Your method to get authenticated user

            // Get all wallets owned by the current user
            List<Wallet> userWallets = walletRepository.findAllByUser(currentUser);
            Set<UUID> userWalletIds = userWallets.stream()
                    .map(Wallet::getWalletID)
                    .collect(Collectors.toSet());

            // Fetch transactions filtered by date and wallet ownership
            List<Transaction> transactions = transactionRepository.findAllByWalletUser(currentUser).stream()
                    .filter(t -> !t.getTransactionDate().toLocalDate().isBefore(startDate) &&
                            !t.getTransactionDate().toLocalDate().isAfter(endDate) &&
                            userWalletIds.contains(t.getWallet().getWalletID()))
                    .toList();

            // Separate income and expenses
            List<Transaction> incomeTransactions = transactions.stream()
                    .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) > 0)
                    .toList();
            List<Transaction> expenseTransactions = transactions.stream()
                    .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
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

                        return new CategoryBreakdownDTO(
                                categoryName,
                                categoryIncome,
                                categoryExpense,
                                incomePercentage,
                                expensePercentage
                        );
                    })
                    .sorted(Comparator.comparing(dto -> dto.getTotalIncome().add(dto.getTotalExpense()), Comparator.reverseOrder()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error generating category breakdown", e);
            throw new RuntimeException("Failed to generate category breakdown", e);
        }
    }

    @Transactional(readOnly = true)
    public DailySummaryDTO getDailySummary(LocalDate date) {
        try {
            log.info("Generating daily summary for date {}", date);

            User currentUser = HelperFunctions.getCurrentUser(userRepository);

            // Get all wallets owned by the current user
            List<UUID> userWalletIds = walletRepository.findAllByUser(currentUser)
                    .stream()
                    .map(Wallet::getWalletID)
                    .toList();

            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

            List<Transaction> transactions = transactionRepository.findAllByWalletUserAndTransactionDateBetween(
                    currentUser, startOfDay, endOfDay);

            // Group transactions by DayOfWeek (though for single day it will be one group)
            Map<DayOfWeek, List<Transaction>> groupedByDay = transactions.stream()
                    .collect(Collectors.groupingBy(t -> t.getTransactionDate().getDayOfWeek()));

            List<DailyDetailDTO> dailyDetails = groupedByDay.entrySet().stream()
                    .map(entry -> {
                        DayOfWeek dayOfWeek = entry.getKey();
                        List<Transaction> txns = entry.getValue();

                        BigDecimal income = txns.stream()
                                .map(Transaction::getAmount)
                                .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        BigDecimal expense = txns.stream()
                                .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                                .map(t -> t.getAmount().abs())
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        DailyDetailDTO detail = new DailyDetailDTO();
                        detail.setDayOfWeek(dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH));
                        detail.setIncome(income);
                        detail.setExpense(expense);
                        return detail;
                    })
                    .collect(Collectors.toList());

            BigDecimal totalIncome = transactions.stream()
                    .map(Transaction::getAmount)
                    .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalExpenses = transactions.stream()
                    .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                    .map(t -> t.getAmount().abs())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            DailySummaryDTO result = new DailySummaryDTO();
            result.setDate(date);
            result.setDayOfWeek(date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
            result.setMonth(date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
            result.setDailyDetails(dailyDetails);
            result.setTotalIncome(totalIncome);
            result.setTotalExpenses(totalExpenses);
            result.setTransactions(transactions.stream()
                    .map(applicationMapper::toTransactionDetailDTO)
                    .collect(Collectors.toList()));

            return result;

        } catch (Exception ex) {
            log.error("Error generating daily summary", ex);
            throw ex;
        }
    }

    public WeeklySummaryDTO getWeeklySummary(LocalDate weekStartDate) {
        try {
            log.info("Generating weekly summary for week starting {}", weekStartDate);

            User user = HelperFunctions.getCurrentUser(userRepository);

            List<UUID> walletIds = walletRepository.findAllByUser(user)
                    .stream()
                    .map(Wallet::getWalletID)
                    .toList();

            LocalDateTime startOfWeek = weekStartDate.atStartOfDay();
            LocalDateTime endOfWeek = startOfWeek.plusDays(7).minusNanos(1);

            List<Transaction> transactions = transactionRepository.findAllByWalletUser(user).stream()
                    .filter(t -> !t.getTransactionDate().isBefore(startOfWeek) && !t.getTransactionDate().isAfter(endOfWeek))
                    .sorted(Comparator.comparing(Transaction::getTransactionDate))
                    .toList();

            int weekOfMonth = (weekStartDate.getDayOfMonth() - 1) / 7 + 1;

            List<WeeklyDetailDTO> weeklyDetails = new ArrayList<>();
            BigDecimal totalIncome = BigDecimal.ZERO;
            BigDecimal totalExpense = BigDecimal.ZERO;

            if (!transactions.isEmpty()) {
                Map<Integer, List<Transaction>> groupedByWeek = transactions.stream()
                        .collect(Collectors.groupingBy(t ->
                                (startOfWeek.getDayOfMonth() + 6 - t.getTransactionDate().getDayOfWeek().getValue()) / 7 + 1
                        ));

                for (Map.Entry<Integer, List<Transaction>> entry : groupedByWeek.entrySet()) {
                    int weekKey = entry.getKey();
                    List<Transaction> group = entry.getValue();

                    BigDecimal income = group.stream()
                            .map(Transaction::getAmount)
                            .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal expense = group.stream()
                            .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                            .map(t -> t.getAmount().abs())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    weeklyDetails.add(new WeeklyDetailDTO(String.valueOf(weekKey), income, expense));

                    totalIncome = totalIncome.add(income);
                    totalExpense = totalExpense.add(expense);
                }
            }

            BigDecimal netCashFlow = totalIncome.subtract(totalExpense);

            Map<String, BigDecimal> dailyTotals = transactions.stream()
                    .collect(Collectors.groupingBy(
                            t -> t.getTransactionDate().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                            LinkedHashMap::new,
                            Collectors.reducing(
                                    BigDecimal.ZERO,
                                    Transaction::getAmount,
                                    BigDecimal::add
                            )
                    ));

            Map<String, BigDecimal> dailyIncomeTotals = transactions.stream()
                    .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) > 0)
                    .collect(Collectors.groupingBy(
                            t -> t.getTransactionDate().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                            LinkedHashMap::new,
                            Collectors.reducing(
                                    BigDecimal.ZERO,
                                    Transaction::getAmount,
                                    BigDecimal::add
                            )
                    ));

            Map<String, BigDecimal> dailyExpenseTotals = transactions.stream()
                    .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                    .collect(Collectors.groupingBy(
                            t -> t.getTransactionDate().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                            LinkedHashMap::new,
                            Collectors.reducing(
                                    BigDecimal.ZERO,
                                    t -> t.getAmount().abs(),
                                    BigDecimal::add
                            )
                    ));

            List<TransactionDetailDTO> transactionDTOs = transactions.stream()
                    .map(applicationMapper::toTransactionDetailDTO)
                    .collect(Collectors.toList());

            return WeeklySummaryDTO.builder()
                    .startDate(startOfWeek.toLocalDate())
                    .endDate(endOfWeek.toLocalDate())
                    .weekNumber(weekOfMonth)
                    .year(weekStartDate.getYear())
                    .weeklyDetails(weeklyDetails)
                    .totalIncome(totalIncome)
                    .totalExpenses(totalExpense)
                    .netCashFlow(netCashFlow)
                    .transactions(transactionDTOs)
                    .dailyTotals(dailyTotals)
                    .dailyIncomeTotals(dailyIncomeTotals)
                    .dailyExpenseTotals(dailyExpenseTotals)
                    .build();

        } catch (Exception ex) {
            log.error("Error generating weekly summary", ex);
            throw new RuntimeException("Weekly summary generation failed", ex);
        }
    }

    public MonthlySummaryDTO getMonthlySummary(YearMonth yearMonth) {
        try {
            log.info("Generating monthly summary for {}-{}", yearMonth.getYear(), yearMonth.getMonthValue());

            User currentUser = HelperFunctions.getCurrentUser(userRepository);

            // Step 1: Get wallet IDs for the current user
            List<UUID> userWalletIds = walletRepository.findAllByUser(currentUser).stream()
                    .map(Wallet::getWalletID)
                    .collect(Collectors.toList());

            // Step 2: Define date range
            LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime endOfMonth = yearMonth.atEndOfMonth().atTime(LocalTime.MAX);

            // Step 3: Fetch transactions
            List<Transaction> transactions = transactionRepository.findByTransactionDateBetweenAndWalletWalletIDInOrderByTransactionDate(
                    startOfMonth, endOfMonth, userWalletIds);

            // Step 4: Monthly detail (usually just one month)
            List<MonthlyDetailDTO> monthlyDetails = transactions.stream()
                    .collect(Collectors.groupingBy(
                            t -> t.getTransactionDate().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                    ))
                    .entrySet().stream()
                    .map(entry -> new MonthlyDetailDTO(
                            entry.getKey(),
                            entry.getValue().stream().map(Transaction::getAmount)
                                    .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0).reduce(BigDecimal.ZERO, BigDecimal::add),
                            entry.getValue().stream().map(Transaction::getAmount)
                                    .filter(amount -> amount.compareTo(BigDecimal.ZERO) < 0).reduce(BigDecimal.ZERO, BigDecimal::add).abs()
                    ))
                    .toList();

            // Step 5: Summary values
            BigDecimal totalIncome = transactions.stream()
                    .map(Transaction::getAmount)
                    .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalExpenses = transactions.stream()
                    .map(Transaction::getAmount)
                    .filter(amount -> amount.compareTo(BigDecimal.ZERO) < 0)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .abs();

            BigDecimal netCashFlow = totalIncome.subtract(totalExpenses);

            // Step 6: Daily totals
            Map<Integer, BigDecimal> dailyTotals = transactions.stream()
                    .collect(Collectors.groupingBy(
                            t -> t.getTransactionDate().getDayOfMonth(),
                            Collectors.mapping(Transaction::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                    ));

            // Step 7: Category totals
            Map<String, BigDecimal> categoryTotals = transactions.stream()
                    .collect(Collectors.groupingBy(
                            t -> t.getCategory().getName(),
                            Collectors.mapping(Transaction::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                    ));

            // Step 8: Mapping to DTO
            List<TransactionDetailDTO> transactionDTOs = transactions.stream()
                    .map(applicationMapper::toTransactionDetailDTO)
                    .toList();

            return new MonthlySummaryDTO(
                    yearMonth.getMonthValue(),
                    yearMonth.getYear(),
                    yearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                    monthlyDetails,
                    totalIncome,
                    totalExpenses,
                    netCashFlow,
                    transactionDTOs,
                    dailyTotals,
                    categoryTotals
            );

        } catch (Exception ex) {
            log.error("Error generating monthly summary", ex);
            throw new RuntimeException("Failed to generate monthly summary", ex);
        }
    }

    public YearlySummaryDTO getYearlySummary(int year) {
        try {
            log.info("Generating yearly summary for year {}", year);

            User currentUser = HelperFunctions.getCurrentUser(userRepository);

            List<UUID> userWalletIds = walletRepository.findAllByUser(currentUser)
                    .stream()
                    .map(Wallet::getWalletID)
                    .toList();

            LocalDateTime startOfYear = LocalDate.of(year, 1, 1).atStartOfDay();
            LocalDateTime endOfYear = LocalDate.of(year, 12, 31).atTime(23, 59, 59);

            List<Transaction> transactions = transactionRepository.findAll()
                    .stream()
                    .filter(t -> userWalletIds.contains(t.getWallet().getWalletID()) &&
                            !t.getTransactionDate().isBefore(startOfYear) &&
                            !t.getTransactionDate().isAfter(endOfYear))
                    .sorted(Comparator.comparing(Transaction::getTransactionDate))
                    .toList();

            // Yearly details
            List<YearlyDetailDTO> yearlyDetails = transactions.stream()
                    .collect(Collectors.groupingBy(t -> t.getTransactionDate().getYear()))
                    .entrySet().stream()
                    .map(e -> new YearlyDetailDTO(
                            String.valueOf(e.getKey()),
                            e.getValue().stream().map(Transaction::getAmount)
                                    .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0).reduce(BigDecimal.ZERO, BigDecimal::add),
                            e.getValue().stream().filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                                    .map(t -> t.getAmount().abs()).reduce(BigDecimal.ZERO, BigDecimal::add)
                    )).toList();

            BigDecimal totalIncome = transactions.stream()
                    .map(Transaction::getAmount)
                    .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0).reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalExpenses = transactions.stream()
                    .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                    .map(t -> t.getAmount().abs()).reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal netCashFlow = totalIncome.subtract(totalExpenses);

            // Monthly totals
            Map<String, BigDecimal> monthlyTotals = transactions.stream()
                    .collect(Collectors.groupingBy(
                            t -> t.getTransactionDate().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                            Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                    ));

            // Category totals
            Map<String, BigDecimal> categoryTotals = transactions.stream()
                    .collect(Collectors.groupingBy(
                            t -> t.getCategory().getName(),
                            Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                    ));

            // Quarterly totals
            Map<String, BigDecimal> quarterlyTotals = new HashMap<>();
            quarterlyTotals.put("Q1", sumForQuarter(transactions, 1, 3));
            quarterlyTotals.put("Q2", sumForQuarter(transactions, 4, 6));
            quarterlyTotals.put("Q3", sumForQuarter(transactions, 7, 9));
            quarterlyTotals.put("Q4", sumForQuarter(transactions, 10, 12));

            List<TransactionDetailDTO> transactionDTOs = transactions.stream()
                    .map(applicationMapper::toTransactionDetailDTO)
                    .toList();

            return new YearlySummaryDTO(
                    year,
                    yearlyDetails,
                    totalIncome,
                    totalExpenses,
                    netCashFlow,
                    transactionDTOs,
                    monthlyTotals,
                    categoryTotals,
                    quarterlyTotals
            );

        } catch (Exception ex) {
            log.error("Error generating yearly summary", ex);
            throw new RuntimeException("Error generating yearly summary", ex);
        }
    }

    private BigDecimal sumForQuarter(List<Transaction> transactions, int startMonth, int endMonth) {
        return transactions.stream()
                .filter(t -> {
                    int month = t.getTransactionDate().getMonthValue();
                    return month >= startMonth && month <= endMonth;
                })
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
