package JavaProject.MoneyManagement_BE_SE330.services.impls;

import JavaProject.MoneyManagement_BE_SE330.helper.ApplicationMapper;
import JavaProject.MoneyManagement_BE_SE330.helper.HelperFunctions;
import JavaProject.MoneyManagement_BE_SE330.helper.ResourceNotFoundException;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.budget.BudgetDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.budget.BudgetProgressDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.budget.CreateBudgetDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.budget.UpdateBudgetDTO;
import JavaProject.MoneyManagement_BE_SE330.models.entities.Budget;
import JavaProject.MoneyManagement_BE_SE330.models.entities.Category;
import JavaProject.MoneyManagement_BE_SE330.models.entities.User;
import JavaProject.MoneyManagement_BE_SE330.models.entities.Wallet;
import JavaProject.MoneyManagement_BE_SE330.repositories.BudgetRepository;
import JavaProject.MoneyManagement_BE_SE330.repositories.CategoryRepository;
import JavaProject.MoneyManagement_BE_SE330.repositories.UserRepository;
import JavaProject.MoneyManagement_BE_SE330.repositories.WalletRepository;
import JavaProject.MoneyManagement_BE_SE330.services.BudgetService;
import lombok.RequiredArgsConstructor;
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

                    // Calculate expected spending based on time elapsed
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
                    String progressStatus;
                    String notification = null;

                    // Not started yet
                    if (currentDateTime.isBefore(startDate)) {
                        progressStatus = "Not Started";
                        notification = String.format("Planning: Your budget for %s will start on %s with a limit of $%.2f.",
                                budget.getCategory().getName(),
                                startDate.toLocalDate(),
                                budget.getLimitAmount());
                    }
                    // Budget completed or deadline passed
                    else if (currentDateTime.isAfter(endDate)) {
                        if (usagePercentage.compareTo(BigDecimal.valueOf(100)) > 0) {
                            progressStatus = "Over Budget";
                            notification = String.format("Budget ended: Your budget for %s exceeded the limit by %.2f%%.",
                                    budget.getCategory().getName(), usagePercentage.subtract(BigDecimal.valueOf(100)));
                        } else if (usagePercentage.compareTo(BigDecimal.valueOf(90)) > 0) {
                            progressStatus = "Nearly Maxed";
                            notification = String.format("Budget ended: Your budget for %s used %.2f%% of the limit.",
                                    budget.getCategory().getName(), usagePercentage);
                        } else {
                            progressStatus = "Under Budget";
                            notification = String.format("Budget ended: Your budget for %s used only %.2f%% of the limit.",
                                    budget.getCategory().getName(), usagePercentage);
                        }
                    }
                    // In progress
                    else {
                        BigDecimal spendingRatio = expectedSpending.compareTo(BigDecimal.ZERO) > 0
                                ? budget.getCurrentSpending().divide(expectedSpending, 4, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;

                        // Calculate remaining budget and daily allowance
                        BigDecimal remainingBudget = budget.getLimitAmount().subtract(budget.getCurrentSpending());
                        BigDecimal dailyAllowance = remainingDays > 0
                                ? remainingBudget.divide(BigDecimal.valueOf(remainingDays), 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;

                        if (spendingRatio.compareTo(BigDecimal.valueOf(1.5)) > 0) {
                            progressStatus = "Critical";
                            notification = String.format("Critical alert: Your spending on %s is significantly higher than expected (%.2f%% vs %.2f%% expected). You have $%.2f left for %d days ($%.2f/day).",
                                    budget.getCategory().getName(), usagePercentage, expectedPercentage,
                                    remainingBudget, remainingDays, dailyAllowance);
                        } else if (spendingRatio.compareTo(BigDecimal.valueOf(1.2)) > 0) {
                            progressStatus = "Warning";
                            notification = String.format("Warning: Your spending on %s is ahead of schedule (%.2f%% vs %.2f%% expected). Try to limit to $%.2f/day for the remaining %d days.",
                                    budget.getCategory().getName(), usagePercentage, expectedPercentage,
                                    dailyAllowance, remainingDays);
                        } else if (spendingRatio.compareTo(BigDecimal.valueOf(0.8)) >= 0) {
                            progressStatus = "On Track";
                            notification = String.format("On track: Your budget for %s is progressing as expected with %.2f%% used. You have $%.2f left until %s.",
                                    budget.getCategory().getName(), usagePercentage,
                                    remainingBudget, endDate.toLocalDate());
                        } else if (spendingRatio.compareTo(BigDecimal.valueOf(0.5)) >= 0) {
                            progressStatus = "Under Budget";
                            notification = String.format("Good job: Your spending on %s is below expected (%.2f%% vs %.2f%% expected). You can spend up to $%.2f/day.",
                                    budget.getCategory().getName(), usagePercentage, expectedPercentage, dailyAllowance);
                        } else {
                            progressStatus = "Minimal Spending";
                            notification = String.format("Very low spending: Your budget for %s has only used %.2f%% when %.2f%% was expected. You have $%.2f remaining.",
                                    budget.getCategory().getName(), usagePercentage, expectedPercentage, remainingBudget);
                        }
                    }

                    dto.setProgressStatus(progressStatus);
                    dto.setNotification(notification);
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
