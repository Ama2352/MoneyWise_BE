package JavaProject.MoneyWise.services.impls;

import JavaProject.MoneyWise.helper.*;
import JavaProject.MoneyWise.models.dtos.budget.*;
import JavaProject.MoneyWise.models.entities.Budget;
import JavaProject.MoneyWise.models.entities.Category;
import JavaProject.MoneyWise.models.entities.User;
import JavaProject.MoneyWise.models.entities.Wallet;
import JavaProject.MoneyWise.repositories.BudgetRepository;
import JavaProject.MoneyWise.repositories.CategoryRepository;
import JavaProject.MoneyWise.repositories.UserRepository;
import JavaProject.MoneyWise.repositories.WalletRepository;
import JavaProject.MoneyWise.services.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {
    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final ApplicationMapper applicationMapper;
    private final MessageSource messageSource;
    private final CurrencyConverter currencyConverter;

    @Transactional
    @Override
    public BudgetDTO createBudget(CreateBudgetDTO model) {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);

        Wallet wallet = walletRepository.findById(model.getWalletId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        if (!wallet.getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not own this wallet");
        }

        Category category = categoryRepository.findById(model.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (!category.getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not own this category");
        }

        if (model.getStartDate().isAfter(model.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }

        // Check for overlapping budgets
        List<Budget> overlaps = budgetRepository
                .findByCategoryAndWalletAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        category, wallet, model.getEndDate(), model.getStartDate());
        if (!overlaps.isEmpty()) {
            throw new IllegalArgumentException("A budget already exists for this category, wallet, and date range");
        }

        Budget budget = applicationMapper.toBudgetEntity(model);
        budgetRepository.save(budget);

        return applicationMapper.toBudgetDTO(budget);
    }

    @Transactional(readOnly = true)
    @Override
    public List<Budget> getAllBudgets() {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        return budgetRepository.findByWalletUser(currentUser);
    }

    @Transactional(readOnly = true)
    @Override
    public BudgetDTO getBudgetById(UUID id) {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
        if (!budget.getWallet().getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not have access to this budget");
        }

        return applicationMapper.toBudgetDTO(budget);
    }

    @Transactional
    @Override
    public BudgetDTO updateBudget(UpdateBudgetDTO model) {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        Budget budget = budgetRepository.findById(model.getBudgetId())
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
        if (!budget.getWallet().getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not have access to this budget");
        }

        Wallet wallet = walletRepository.findById(model.getWalletId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        if (!wallet.getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not own this wallet");
        }

        Category category = categoryRepository.findById(model.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (!category.getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not own this category");
        }

        if (model.getStartDate().isAfter(model.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }

        // Check for overlaps (excluding current budget)
        List<Budget> overlaps = budgetRepository
                .findByCategoryAndWalletAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        category, wallet, model.getEndDate(), model.getStartDate());
        overlaps.remove(budget);
        if (!overlaps.isEmpty()) {
            throw new IllegalArgumentException("A budget already exists for this category, wallet, and date range");
        }

        budget.setCategory(category);
        budget.setWallet(wallet);
        budget.setDescription(model.getDescription());
        budget.setLimitAmount(model.getLimitAmount());
        budget.setStartDate(model.getStartDate());
        budget.setEndDate(model.getEndDate());

        budgetRepository.save(budget);

        return applicationMapper.toBudgetDTO(budget);
    }

    @Transactional
    @Override
    public UUID deleteBudget(UUID id) {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
        if (!budget.getWallet().getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not have access to this budget");
        }
        budgetRepository.delete(budget);
        return id;
    }

    @Transactional(readOnly = true)
    @Override
    public List<BudgetProgressDTO> searchBudgets(SearchBudgetsDTO filter, String acceptLanguage) {
        if (acceptLanguage != null && !acceptLanguage.isEmpty()) {
            LocaleContextHolder.setLocale(Locale.forLanguageTag(acceptLanguage));
        }
        String currency = filter.getCurrency() != null ? filter.getCurrency() : "VND";

        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        LocalDateTime startDateTime = filter.getStartDate() != null ? filter.getStartDate().atStartOfDay() : null;
        LocalDateTime endDateTime = filter.getEndDate() != null ? filter.getEndDate().atTime(LocalTime.MAX) : null;

        List<BudgetProgressDTO> budgetProgress = getBudgetProgressAndAlerts(acceptLanguage, currency);

        // Create a map for quick lookup by budgetId
        Map<UUID, BudgetProgressDTO> progressMap = budgetProgress.stream()
                .collect(Collectors.toMap(BudgetProgressDTO::getBudgetId, dto -> dto));

        List<Budget> budgets = budgetRepository
                .findByWalletUser(currentUser)
                .stream()
                .filter(budget -> {
                    if (startDateTime != null && endDateTime != null)
                        return !budget.getStartDate().isAfter(endDateTime) &&
                                !budget.getEndDate().isBefore(startDateTime);
                    else if (startDateTime != null)
                        return !budget.getStartDate().isBefore(startDateTime);
                    else if (endDateTime != null)
                        return !budget.getEndDate().isAfter(endDateTime);
                    else
                        return true;

                })
                .filter(budget -> filter.getKeywords() == null ||
                        (budget.getDescription() != null &&
                                budget.getDescription().toLowerCase().contains(filter.getKeywords().toLowerCase())))
                .filter(budget -> filter.getCategoryName() == null ||
                        budget.getCategory().getName().equalsIgnoreCase(filter.getCategoryName()))
                .filter(budget -> filter.getWalletName() == null ||
                        budget.getWallet().getWalletName().equalsIgnoreCase(filter.getWalletName()))
                .filter(budget -> {
                    BigDecimal min = filter.getMinLimitAmount();
                    BigDecimal max = filter.getMaxLimitAmount();
                    if(min != null && max != null)
                        return budget.getLimitAmount().compareTo(min) >= 0 &&
                                budget.getLimitAmount().compareTo(max) <= 0;
                    else if (min != null)
                        return budget.getLimitAmount().compareTo(min) >= 0;
                    else if (max != null)
                        return budget.getLimitAmount().compareTo(max) <= 0;
                    else
                        return true;
                })
                .sorted(Comparator.comparing(Budget::getCreatedAt).reversed())
                .toList();

        // Map filtered budgets to their corresponding BudgetProgressDTOs
        return budgets.stream()
                .map(budget -> progressMap.get(budget.getBudgetId()))
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<BudgetProgressDTO> getBudgetProgressAndAlerts(String acceptLanguage, String currency) {
        if (acceptLanguage != null && !acceptLanguage.isEmpty()) {
            LocaleContextHolder.setLocale(Locale.forLanguageTag(acceptLanguage));
        }
        if( currency == null || currency.isEmpty()) {
            currency = "VND"; // Default to VND if no currency is provided
        }
        BigDecimal exchangeRateUSDtoVND = currencyConverter.fetchExchangeRate();

        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        LocalDateTime currentDateTime = LocalDateTime.now();

        String finalCurrency = currency;
        return budgetRepository.findByWalletUser(currentUser).stream()
                .map(budget -> {
                    BudgetProgressDTO dto = applicationMapper.toBudgetProgressDTO(budget);

                    // Calculate usage percentage
                    BigDecimal usagePercentage = BigDecimal.ZERO;
                    if (budget.getLimitAmount().compareTo(BigDecimal.ZERO) > 0) {
                        usagePercentage = budget.getCurrentSpending()
                                .divide(budget.getLimitAmount(), 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .setScale(2, RoundingMode.HALF_UP);
                    }
                    dto.setUsagePercentage(usagePercentage);

                    // Calculate expected spending
                    LocalDateTime endDate = budget.getEndDate();
                    LocalDateTime startDate = budget.getStartDate();
                    LocalDateTime effectiveDate = currentDateTime.isAfter(endDate) ? endDate : currentDateTime;

                    long totalDays = ChronoUnit.DAYS.between(startDate, endDate);
                    long elapsedDays = ChronoUnit.DAYS.between(startDate, effectiveDate);
                    BigDecimal progressRatio = totalDays > 0
                            ? BigDecimal.valueOf(elapsedDays).divide(BigDecimal.valueOf(totalDays), 4,
                                    RoundingMode.HALF_UP)
                            : BigDecimal.ONE;
                    BigDecimal expectedSpending = budget.getLimitAmount().multiply(progressRatio);
                    BigDecimal expectedPercentage = progressRatio.multiply(BigDecimal.valueOf(100)).setScale(2,
                            RoundingMode.HALF_UP);

                    // Get remaining days
                    long remainingDays = ChronoUnit.DAYS.between(currentDateTime, endDate);

                    // Determine progress status and notification
                    String progressStatus = determineProgressStatus(
                            currentDateTime,
                            budget,
                            usagePercentage,
                            expectedSpending
                    );

                    String notification = generateNotification(
                            progressStatus,
                            budget,
                            usagePercentage,
                            expectedPercentage,
                            remainingDays,
                            finalCurrency,
                            exchangeRateUSDtoVND,
                            acceptLanguage
                    );

                    // Localize the progress status
                    String localizedProgressStatus = messageSource.getMessage(
                            "budget.status." + progressStatus.toLowerCase().replace(" ", "."),
                            null,
                            LocaleContextHolder.getLocale());

                    dto.setProgressStatus(localizedProgressStatus);
                    dto.setNotification(notification);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private String determineProgressStatus(
            LocalDateTime currentDateTime,
            Budget budget,
            BigDecimal usagePercentage,
            BigDecimal expectedSpending
    ) {
        if (currentDateTime.isBefore(budget.getStartDate())) {
            return "Not Started";
        }

        if (currentDateTime.isAfter(budget.getEndDate())) {
            if (usagePercentage.compareTo(BigDecimal.valueOf(100)) > 0) {
                return "Over Budget";
            } else if (usagePercentage.compareTo(BigDecimal.valueOf(90)) > 0) {
                return "Nearly Maxed";
            } else {
                return "Under Budget";
            }
        }

        BigDecimal spendingRatio = expectedSpending.compareTo(BigDecimal.ZERO) > 0
                ? budget.getCurrentSpending().divide(expectedSpending, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        if (spendingRatio.compareTo(BigDecimal.valueOf(1.5)) > 0) {
            return "Critical";
        } else if (spendingRatio.compareTo(BigDecimal.valueOf(1.2)) > 0) {
            return "Warning";
        } else if (spendingRatio.compareTo(BigDecimal.valueOf(0.8)) >= 0) {
            return "On Track";
        } else if (spendingRatio.compareTo(BigDecimal.valueOf(0.5)) >= 0) {
            return "Under Budget";
        } else {
            return "Minimal Spending";
        }
    }

    private String generateNotification(
            String progressStatus,
            Budget budget,
            BigDecimal usagePercentage,
            BigDecimal expectedPercentage,
            long remainingDays,
            String currency,
            BigDecimal exchangeRateUSDtoVND,
            String acceptLanguage
    ) {
        String categoryName = budget.getCategory().getName();

        String startDate = DateTimeFormatterUtil.formatDateTimeWithLanguage(budget.getStartDate(), acceptLanguage);
        String endDate = DateTimeFormatterUtil.formatDateTimeWithLanguage(budget.getEndDate(), acceptLanguage);

        // Calculate amounts
        BigDecimal limitAmount = currency.equalsIgnoreCase("USD")
                ? currencyConverter.convertVNDtoUSD(budget.getLimitAmount(), exchangeRateUSDtoVND)
                : budget.getLimitAmount();
        BigDecimal currentSpending = currency.equalsIgnoreCase("USD")
                ? currencyConverter.convertVNDtoUSD(budget.getCurrentSpending(), exchangeRateUSDtoVND)
                : budget.getCurrentSpending();

        // Format amounts for display
        String displayedLimitAmount = currencyConverter.formatAmountToDisplay(limitAmount, currency);
        String displayedRemainingBudget = currencyConverter.formatAmountToDisplay(
                limitAmount.subtract(currentSpending), currency);

        BigDecimal remainingBudget = limitAmount.subtract(currentSpending);
        BigDecimal dailyAllowance = remainingDays > 0
                ? remainingBudget.divide(BigDecimal.valueOf(remainingDays), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        String displayedDailyAllowance = currencyConverter.formatAmountToDisplay(dailyAllowance, currency);

        return switch (progressStatus) {
            case "Not Started" -> messageSource.getMessage(
                    "budget.not.started",
                    new Object[]{categoryName, startDate, displayedLimitAmount},
                    LocaleContextHolder.getLocale());
            case "Over Budget" -> messageSource.getMessage(
                    "budget.over.budget",
                    new Object[]{categoryName, usagePercentage.subtract(BigDecimal.valueOf(100))},
                    LocaleContextHolder.getLocale());
            case "Nearly Maxed" -> messageSource.getMessage(
                    "budget.nearly.maxed",
                    new Object[]{categoryName, usagePercentage},
                    LocaleContextHolder.getLocale());
            case "Under Budget" -> messageSource.getMessage(
                    "budget.under.budget",
                    new Object[]{categoryName, usagePercentage},
                    LocaleContextHolder.getLocale());
            case "Critical" -> messageSource.getMessage(
                    "budget.critical",
                    new Object[]{categoryName, usagePercentage, expectedPercentage, displayedRemainingBudget,
                            remainingDays, displayedDailyAllowance},
                    LocaleContextHolder.getLocale());
            case "Warning" -> messageSource.getMessage(
                    "budget.warning",
                    new Object[]{categoryName, usagePercentage, expectedPercentage, displayedDailyAllowance,
                            remainingDays},
                    LocaleContextHolder.getLocale());
            case "On Track" -> messageSource.getMessage(
                    "budget.on.track",
                    new Object[]{categoryName, usagePercentage, displayedRemainingBudget, endDate},
                    LocaleContextHolder.getLocale());
            case "Minimal Spending" -> messageSource.getMessage(
                    "budget.minimal.spending",
                    new Object[]{categoryName, usagePercentage, expectedPercentage, displayedRemainingBudget},
                    LocaleContextHolder.getLocale());
            default -> "";
        };
    }
}
