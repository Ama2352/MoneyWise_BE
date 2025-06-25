package JavaProject.MoneyWise.services.impls;

import JavaProject.MoneyWise.helper.ApplicationMapper;
import JavaProject.MoneyWise.helper.HelperFunctions;
import JavaProject.MoneyWise.helper.ResourceNotFoundException;
import JavaProject.MoneyWise.models.dtos.budget.BudgetDTO;
import JavaProject.MoneyWise.models.dtos.budget.BudgetProgressDTO;
import JavaProject.MoneyWise.models.dtos.budget.CreateBudgetDTO;
import JavaProject.MoneyWise.models.dtos.budget.UpdateBudgetDTO;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
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
        List<Budget> overlaps = budgetRepository.findByCategoryAndWalletAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
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
        List<Budget> overlaps = budgetRepository.findByCategoryAndWalletAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
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
    public List<BudgetProgressDTO> getBudgetProgressAndAlerts() {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        LocalDateTime currentDateTime = LocalDateTime.now();

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
                            ? BigDecimal.valueOf(elapsedDays).divide(BigDecimal.valueOf(totalDays), 4, RoundingMode.HALF_UP)
                            : BigDecimal.ONE;
                    BigDecimal expectedSpending = budget.getLimitAmount().multiply(progressRatio);
                    BigDecimal expectedPercentage = progressRatio.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);

                    // Get remaining days
                    long remainingDays = ChronoUnit.DAYS.between(currentDateTime, endDate);

                    // Determine progress status and notification
                    String progressStatus = determineProgressStatus(
                            currentDateTime,
                            budget,
                            usagePercentage,
                            expectedSpending,
                            remainingDays
                    );

                    String notification = generateNotification(
                            progressStatus,
                            budget,
                            usagePercentage,
                            expectedPercentage,
                            remainingDays,
                            expectedSpending
                    );

                    // Localize the progress status
                    String localizedProgressStatus = messageSource.getMessage(
                            "budget.status." + progressStatus.toLowerCase().replace(" ", "."),
                            null,
                            LocaleContextHolder.getLocale()
                    );

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
            BigDecimal expectedSpending,
            long remainingDays
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
            BigDecimal expectedSpending
    ) {
        String categoryName = budget.getCategory().getName();
        String startDate = budget.getStartDate().toLocalDate().toString();
        String endDate = budget.getEndDate().toLocalDate().toString();
        BigDecimal limitAmount = budget.getLimitAmount();
        BigDecimal currentSpending = budget.getCurrentSpending();
        BigDecimal remainingBudget = limitAmount.subtract(currentSpending);
        BigDecimal dailyAllowance = remainingDays > 0
                ? remainingBudget.divide(BigDecimal.valueOf(remainingDays), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        switch (progressStatus) {
            case "Not Started":
                return messageSource.getMessage(
                        "budget.not.started",
                        new Object[]{categoryName, startDate, limitAmount},
                        LocaleContextHolder.getLocale()
                );

            case "Over Budget":
                return messageSource.getMessage(
                        "budget.over.budget",
                        new Object[]{categoryName, usagePercentage.subtract(BigDecimal.valueOf(100))},
                        LocaleContextHolder.getLocale()
                );

            case "Nearly Maxed":
                return messageSource.getMessage(
                        "budget.nearly.maxed",
                        new Object[]{categoryName, usagePercentage},
                        LocaleContextHolder.getLocale()
                );

            case "Under Budget":
                return messageSource.getMessage(
                        "budget.under.budget",
                        new Object[]{categoryName, usagePercentage},
                        LocaleContextHolder.getLocale()
                );

            case "Critical":
                return messageSource.getMessage(
                        "budget.critical",
                        new Object[]{categoryName, usagePercentage, expectedPercentage, remainingBudget, remainingDays, dailyAllowance},
                        LocaleContextHolder.getLocale()
                );

            case "Warning":
                return messageSource.getMessage(
                        "budget.warning",
                        new Object[]{categoryName, usagePercentage, expectedPercentage, dailyAllowance, remainingDays},
                        LocaleContextHolder.getLocale()
                );

            case "On Track":
                return messageSource.getMessage(
                        "budget.on.track",
                        new Object[]{categoryName, usagePercentage, remainingBudget, endDate},
                        LocaleContextHolder.getLocale()
                );

            case "Minimal Spending":
                return messageSource.getMessage(
                        "budget.minimal.spending",
                        new Object[]{categoryName, usagePercentage, expectedPercentage, remainingBudget},
                        LocaleContextHolder.getLocale()
                );

            default:
                return "";
        }
    }
}
