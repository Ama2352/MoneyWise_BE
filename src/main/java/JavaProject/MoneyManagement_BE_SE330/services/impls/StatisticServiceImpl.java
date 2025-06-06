package JavaProject.MoneyManagement_BE_SE330.services.impls;

import JavaProject.MoneyManagement_BE_SE330.helper.HelperFunctions;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.report.ReportInfoDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.statistic.*;
import JavaProject.MoneyManagement_BE_SE330.models.entities.*;
import JavaProject.MoneyManagement_BE_SE330.repositories.*;
import JavaProject.MoneyManagement_BE_SE330.services.StatisticService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RequiredArgsConstructor
@Slf4j
@Service
public class StatisticServiceImpl implements StatisticService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final BudgetRepository budgetRepository;
    private final SavingGoalRepository savingGoalRepository;

    private static final String TRANSACTION_TYPE_INCOME = "income";
    private static final String TRANSACTION_TYPE_EXPENSE = "expense";

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
                    .filter(t -> TRANSACTION_TYPE_INCOME.equalsIgnoreCase(t.getType()))
                    .toList();

            List<Transaction> expenseTransactions = transactions.stream()
                    .filter(t -> TRANSACTION_TYPE_EXPENSE.equalsIgnoreCase(t.getType()))
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
                                .filter(t -> TRANSACTION_TYPE_INCOME.equalsIgnoreCase(t.getType()))
                                .map(Transaction::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        BigDecimal categoryExpense = categoryTransactions.stream()
                                .filter(t -> TRANSACTION_TYPE_EXPENSE.equalsIgnoreCase(t.getType()))
                                .map(Transaction::getAmount)
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
    public CashFlowSummaryDTO getCashFlowSummary(
            @Nullable LocalDate startDate,
            @Nullable LocalDate endDate
    ) {
        try {
            log.info("Generating cash flow summary from {} to {}", startDate, endDate);
            User currentUser = HelperFunctions.getCurrentUser(userRepository);
            LocalDateTime startDateTime =
                    startDate != null ? startDate.atStartOfDay() : null;
            LocalDateTime endDateTime =
                    endDate != null ? endDate.atTime(LocalTime.MAX) : null;


            List<UUID> userWalletIds = walletRepository.findAllByUser(currentUser)
                    .stream()
                    .map(Wallet::getWalletId)
                    .collect(Collectors.toList());


            List<Transaction> transactions;
            if (startDateTime == null && endDateTime == null) {
                transactions = transactionRepository.findAllByWalletUser(currentUser);
            } else if (startDateTime != null && endDateTime == null) {
                transactions = transactionRepository.findByWalletWalletIdInAndTransactionDateAfter(
                        userWalletIds,
                        startDateTime
                );
            } else if (startDateTime == null) {
                transactions = transactionRepository.findByWalletWalletIdInAndTransactionDateBefore(
                        userWalletIds,
                        endDateTime
                );
            } else {
                transactions = transactionRepository.findByWalletWalletIdInAndTransactionDateBetween(
                        userWalletIds,
                        startDateTime,
                        endDateTime
                );
            }

            BigDecimal totalIncome = transactions.stream()
                    .filter(t -> TRANSACTION_TYPE_INCOME.equalsIgnoreCase(t.getType()))
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalExpenses = transactions.stream()
                    .filter(t -> TRANSACTION_TYPE_EXPENSE.equalsIgnoreCase(t.getType()))
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return new CashFlowSummaryDTO(totalIncome, totalExpenses);

        } catch (Exception e) {
            log.error("Error generating cash flow summary", e);
            throw new RuntimeException("Failed to generate cash flow summary", e);
        }
    }

    @Override
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
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        // 3) fetch daily transactions (for totals)
        List<Transaction> dailyTxs = transactionRepository
                .findByWalletWalletIdInAndTransactionDateBetween(
                        walletIds, startOfDay, endOfDay);

        // 4) compute totalIncome & totalExpenses by type
        BigDecimal totalIncome = dailyTxs.stream()
                .filter(tx -> TRANSACTION_TYPE_INCOME.equalsIgnoreCase(tx.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = dailyTxs.stream()
                .filter(tx -> TRANSACTION_TYPE_EXPENSE.equalsIgnoreCase(tx.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5) define current week (Monday → next Monday)
        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(7);
        LocalDateTime weekStartDT = weekStart.atStartOfDay();
        LocalDateTime weekEndDT = weekEnd.atStartOfDay();

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
                            .filter(tx -> TRANSACTION_TYPE_INCOME.equalsIgnoreCase(tx.getType()))
                            .map(Transaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal expense = txs.stream()
                            .filter(tx -> TRANSACTION_TYPE_EXPENSE.equalsIgnoreCase(tx.getType()))
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

            // SỬA ĐỔI TẠI ĐÂY: Sử dụng equalsIgnoreCase
            BigDecimal income = weekTxs.stream()
                    .filter(t -> TRANSACTION_TYPE_INCOME.equalsIgnoreCase(t.getType()))
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // SỬA ĐỔI TẠI ĐÂY: Sử dụng equalsIgnoreCase
            BigDecimal expense = weekTxs.stream()
                    .filter(t -> TRANSACTION_TYPE_EXPENSE.equalsIgnoreCase(t.getType()))
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

        // 2. Define year range (This seems to be for the *entire* year, not just the month)
        // If you only want the specific month, you can adjust the fetch range.
        // For current logic that fetches whole year then filters by month:
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

                    // SỬA ĐỔI TẠI ĐÂY: Sử dụng equalsIgnoreCase
                    BigDecimal income = monthlyTxs.stream()
                            .filter(t -> TRANSACTION_TYPE_INCOME.equalsIgnoreCase(t.getType()))
                            .map(Transaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    // SỬA ĐỔI TẠI ĐÂY: Sử dụng equalsIgnoreCase
                    BigDecimal expense = monthlyTxs.stream()
                            .filter(t -> TRANSACTION_TYPE_EXPENSE.equalsIgnoreCase(t.getType()))
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
                // SỬA ĐỔI TẠI ĐÂY: Sử dụng equalsIgnoreCase
                .filter(t -> TRANSACTION_TYPE_INCOME.equalsIgnoreCase(t.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = yearTransactions.stream()
                .filter(t -> {
                    LocalDate d = t.getTransactionDate().toLocalDate();
                    return !d.isBefore(startOfMonth) && !d.isAfter(endOfMonth);
                })
                // SỬA ĐỔI TẠI ĐÂY: Sử dụng equalsIgnoreCase
                .filter(t -> TRANSACTION_TYPE_EXPENSE.equalsIgnoreCase(t.getType()))
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
        // Ensure to handle cases where userWalletIds is empty
        if (userWalletIds.isEmpty()) {
            YearlySummaryDTO empty = new YearlySummaryDTO();
            empty.setYearlyDetails(new ArrayList<>());
            empty.setTotalIncome(BigDecimal.ZERO);
            empty.setTotalExpenses(BigDecimal.ZERO);
            return empty;
        }

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

                    // SỬA ĐỔI TẠI ĐÂY: Sử dụng equalsIgnoreCase
                    BigDecimal income = yearTxs.stream()
                            .filter(t -> TRANSACTION_TYPE_INCOME.equalsIgnoreCase(t.getType()))
                            .map(Transaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    // SỬA ĐỔI TẠI ĐÂY: Sử dụng equalsIgnoreCase
                    BigDecimal expense = yearTxs.stream()
                            .filter(t -> TRANSACTION_TYPE_EXPENSE.equalsIgnoreCase(t.getType()))
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

        // SỬA ĐỔI TẠI ĐÂY: Sử dụng equalsIgnoreCase
        BigDecimal totalIncome = currentYearTxs.stream()
                .filter(t -> TRANSACTION_TYPE_INCOME.equalsIgnoreCase(t.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // SỬA ĐỔI TẠI ĐÂY: Sử dụng equalsIgnoreCase
        BigDecimal totalExpenses = currentYearTxs.stream()
                .filter(t -> TRANSACTION_TYPE_EXPENSE.equalsIgnoreCase(t.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add).abs();

        // 6. Create and return summary DTO
        YearlySummaryDTO result = new YearlySummaryDTO();
        result.setYearlyDetails(yearlyDetails);
        result.setTotalIncome(totalIncome);
        result.setTotalExpenses(totalExpenses);
        return result;
    }

    @Override
    public Object generateReportData(ReportInfoDTO reportInfo, String acceptLanguage) {
        try {
            switch (reportInfo.getType().toLowerCase()) {
                case "category-breakdown":
                    return getCategoryBreakdown(reportInfo.getStartDate(), reportInfo.getEndDate());
                case "cash-flow":
                    return getCashFlowSummary(reportInfo.getStartDate(), reportInfo.getEndDate());
                case "daily-summary":
                    return getDailySummary(reportInfo.getStartDate());
                case "weekly-summary":
                    return getWeeklySummary(reportInfo.getStartDate());
                case "monthly-summary":
                    return getMonthlySummary(YearMonth.from(reportInfo.getStartDate()));
                case "yearly-summary":
                    return getYearlySummary(reportInfo.getStartDate().getYear());
                default:
                    throw new IllegalArgumentException("Unsupported report type: " + reportInfo.getType());
            }
        } catch (Exception e) {
            log.error("Error generating report data for type: {}", reportInfo.getType(), e);
            throw new RuntimeException("Failed to generate report data", e);
        }
    }
}