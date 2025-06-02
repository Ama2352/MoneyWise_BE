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

        Wallet wallet = walletRepository.findById(model.getWalletID())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        if (!wallet.getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not own this wallet");
        }

        Category category = categoryRepository.findById(model.getCategoryID())
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

        Wallet wallet = walletRepository.findById(model.getWalletID())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        if (!wallet.getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not own this wallet");
        }

        Category category = categoryRepository.findById(model.getCategoryID())
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
        LocalDateTime currentDate = LocalDateTime.now(); // May 26, 2025, 8:31 PM +07

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

                    // Determine progress status
                    String progressStatus;
                    String notification = null;
                    if (currentDate.isBefore(budget.getStartDate())) {
                        progressStatus = "Pending";
                    } else if (currentDate.isAfter(budget.getEndDate())) {
                        progressStatus = "Complete";
                        // Notifications for completed budgets
                        if (usagePercentage.compareTo(BigDecimal.valueOf(80)) >= 0 && usagePercentage.compareTo(BigDecimal.valueOf(100)) <= 0) {
                            notification = String.format("Completed: Budget for category %s ended at %.2f%% of the limit.",
                                    budget.getCategory().getName(), usagePercentage);
                        } else if (usagePercentage.compareTo(BigDecimal.valueOf(100)) > 0) {
                            notification = String.format("Completed: Budget for category %s exceeded the limit by %.2f%%!",
                                    budget.getCategory().getName(), usagePercentage);
                        }
                    } else {
                        // Active budget (startDate <= currentDate <= endDate)
                        if (usagePercentage.compareTo(BigDecimal.valueOf(80)) < 0) {
                            progressStatus = "Safe";
                        } else if (usagePercentage.compareTo(BigDecimal.valueOf(100)) <= 0) {
                            progressStatus = "Warning";
                            notification = String.format("Warning: Budget for category %s is at %.2f%% of the limit.",
                                    budget.getCategory().getName(), usagePercentage);
                        } else {
                            progressStatus = "Over Budget";
                            notification = String.format("Alert: Budget for category %s has exceeded the limit by %.2f%%!",
                                    budget.getCategory().getName(), usagePercentage);
                        }
                    }
                    dto.setProgressStatus(progressStatus);
                    dto.setNotification(notification);

                    return dto;
                })
                .collect(Collectors.toList());
    }
}
