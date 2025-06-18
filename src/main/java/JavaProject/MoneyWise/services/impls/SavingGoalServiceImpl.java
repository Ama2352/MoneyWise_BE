package JavaProject.MoneyWise.services.impls;

import JavaProject.MoneyWise.helper.ApplicationMapper;
import JavaProject.MoneyWise.helper.HelperFunctions;
import JavaProject.MoneyWise.helper.ResourceNotFoundException;
import JavaProject.MoneyWise.models.dtos.savingGoal.CreateSavingGoalDTO;
import JavaProject.MoneyWise.models.dtos.savingGoal.SavingGoalDTO;
import JavaProject.MoneyWise.models.dtos.savingGoal.SavingGoalProgressDTO;
import JavaProject.MoneyWise.models.dtos.savingGoal.UpdateSavingGoalDTO;
import JavaProject.MoneyWise.models.entities.Category;
import JavaProject.MoneyWise.models.entities.SavingGoal;
import JavaProject.MoneyWise.models.entities.User;
import JavaProject.MoneyWise.models.entities.Wallet;
import JavaProject.MoneyWise.repositories.CategoryRepository;
import JavaProject.MoneyWise.repositories.SavingGoalRepository;
import JavaProject.MoneyWise.repositories.UserRepository;
import JavaProject.MoneyWise.repositories.WalletRepository;
import JavaProject.MoneyWise.services.SavingGoalService;
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
public class SavingGoalServiceImpl implements SavingGoalService {
    private final SavingGoalRepository savingGoalRepository;
    private final CategoryRepository categoryRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final ApplicationMapper applicationMapper;

    @Transactional
    @Override
    public SavingGoalDTO createSavingGoal(CreateSavingGoalDTO model) {
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

        // Check for overlapping saving goals
        List<SavingGoal> overlaps = savingGoalRepository.findByCategoryAndWalletAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                category, wallet, model.getEndDate(), model.getStartDate());
        if (!overlaps.isEmpty()) {
            throw new IllegalArgumentException("A saving goal already exists for this category, wallet, and date range");
        }

        SavingGoal savingGoal = applicationMapper.toSavingGoalEntity(model);
        savingGoalRepository.save(savingGoal);

        return applicationMapper.toSavingGoalDTO(savingGoal);
    }

    @Transactional(readOnly = true)
    @Override
    public List<SavingGoalDTO> getAllSavingGoals() {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        return savingGoalRepository.findByWalletUser(currentUser)
                .stream()
                .map(applicationMapper::toSavingGoalDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public SavingGoalDTO getSavingGoalById(UUID id) {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        SavingGoal savingGoal = savingGoalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Saving goal not found"));
        if (!savingGoal.getWallet().getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not have access to this saving goal");
        }

        return applicationMapper.toSavingGoalDTO(savingGoal);
    }

    @Transactional
    @Override
    public SavingGoalDTO updateSavingGoal(UpdateSavingGoalDTO model) {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        SavingGoal savingGoal = savingGoalRepository.findById(model.getSavingGoalId())
                .orElseThrow(() -> new ResourceNotFoundException("Saving goal not found"));
        if (!savingGoal.getWallet().getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not have access to this saving goal");
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

        // Check for overlaps (excluding current saving goal)
        List<SavingGoal> overlaps = savingGoalRepository.findByCategoryAndWalletAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                category, wallet, model.getEndDate(), model.getStartDate());
        overlaps.remove(savingGoal);
        if (!overlaps.isEmpty()) {
            throw new IllegalArgumentException("A saving goal already exists for this category, wallet, and date range");
        }

        savingGoal.setCategory(category);
        savingGoal.setWallet(wallet);
        savingGoal.setDescription(model.getDescription());
        savingGoal.setTargetAmount(model.getTargetAmount());
        savingGoal.setStartDate(model.getStartDate());
        savingGoal.setEndDate(model.getEndDate());

        savingGoalRepository.save(savingGoal);

        return applicationMapper.toSavingGoalDTO(savingGoal);
    }

    @Transactional
    @Override
    public UUID deleteSavingGoal(UUID id) {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        SavingGoal savingGoal = savingGoalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Saving goal not found"));
        if (!savingGoal.getWallet().getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not have access to this saving goal");
        }
        savingGoalRepository.delete(savingGoal);
        return id;
    }

    @Transactional(readOnly = true)
    @Override
    public List<SavingGoalProgressDTO> getSavingGoalProgressAndAlerts() {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        LocalDateTime currentDateTime = LocalDateTime.now();

        return savingGoalRepository.findByWalletUser(currentUser).stream()
                .map(savingGoal -> {
                    SavingGoalProgressDTO dto = applicationMapper.toSavingGoalProgressDTO(savingGoal);

                    // Calculate saved percentage
                    BigDecimal savedPercentage = BigDecimal.ZERO;
                    if (savingGoal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
                        savedPercentage = savingGoal.getSavedAmount()
                                .divide(savingGoal.getTargetAmount(), 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .setScale(2, RoundingMode.HALF_UP);
                    }
                    dto.setSavedPercentage(savedPercentage);

                    // Calculate expected savings based on time elapsed
                    LocalDateTime endDate = savingGoal.getEndDate();
                    LocalDateTime effectiveDate = currentDateTime.isAfter(endDate) ? endDate : currentDateTime;
                    long totalDays = ChronoUnit.DAYS.between(savingGoal.getStartDate(), endDate);
                    long elapsedDays = ChronoUnit.DAYS.between(savingGoal.getStartDate(), effectiveDate);
                    BigDecimal progressRatio = totalDays > 0
                            ? BigDecimal.valueOf(elapsedDays).divide(BigDecimal.valueOf(totalDays), 4, RoundingMode.HALF_UP)
                            : BigDecimal.ONE;
                    BigDecimal expectedSavedAmount = savingGoal.getTargetAmount().multiply(progressRatio);
                    BigDecimal expectedPercentage = progressRatio.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);

                    // Get remaining days
                    long remainingDays = ChronoUnit.DAYS.between(currentDateTime, endDate);

                    // Determine progress status and notification
                    String progressStatus;
                    String notification = null;

                    // Not started yet
                    if (currentDateTime.isBefore(savingGoal.getStartDate())) {
                        progressStatus = "Not Started";
                        notification = String.format("Planning: Your saving goal for %s will start on %s with a target of %.2f.",
                                savingGoal.getCategory().getName(),
                                savingGoal.getStartDate().toLocalDate(),
                                savingGoal.getTargetAmount());
                    }
                    // Goal completed or deadline passed
                    else if (currentDateTime.isAfter(endDate)) {
                        if (savedPercentage.compareTo(BigDecimal.valueOf(100)) >= 0) {
                            progressStatus = "Achieved";
                            notification = String.format("Success: You've achieved your saving goal for %s with %.2f%% of your target!",
                                    savingGoal.getCategory().getName(), savedPercentage);
                        } else if (savedPercentage.compareTo(BigDecimal.valueOf(75)) >= 0) {
                            progressStatus = "Partially Achieved";
                            notification = String.format("Almost there: Your saving goal for %s reached %.2f%% of the target.",
                                    savingGoal.getCategory().getName(), savedPercentage);
                        } else {
                            progressStatus = "Missed Target";
                            notification = String.format("Goal ended: Your saving goal for %s reached only %.2f%% of the target.",
                                    savingGoal.getCategory().getName(), savedPercentage);
                        }
                    }
                    // Goal already achieved before deadline
                    else if (savingGoal.getSavedAmount().compareTo(savingGoal.getTargetAmount()) >= 0) {
                        progressStatus = "Achieved Early";
                        notification = String.format("Excellent! You've already achieved your saving goal for %s with %.2f%% and still have %d days remaining.",
                                savingGoal.getCategory().getName(), savedPercentage, remainingDays);
                    }
                    // In progress
                    else {
                        BigDecimal progressRatio2 = expectedSavedAmount.compareTo(BigDecimal.ZERO) > 0
                                ? savingGoal.getSavedAmount().divide(expectedSavedAmount, 4, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;

                        if (progressRatio2.compareTo(BigDecimal.valueOf(1.2)) > 0) {
                            progressStatus = "Ahead";
                            notification = String.format("Great progress! You're ahead on your %s saving goal (%.2f%% saved vs %.2f%% expected).",
                                    savingGoal.getCategory().getName(), savedPercentage, expectedPercentage);
                        } else if (progressRatio2.compareTo(BigDecimal.valueOf(0.8)) >= 0) {
                            progressStatus = "On Track";
                            notification = String.format("On track: Your saving goal for %s is progressing well with %.2f%% saved. %.2f more needed by %s.",
                                    savingGoal.getCategory().getName(), savedPercentage,
                                    savingGoal.getTargetAmount().subtract(savingGoal.getSavedAmount()),
                                    endDate.toLocalDate());
                        } else if (progressRatio2.compareTo(BigDecimal.valueOf(0.6)) >= 0) {
                            progressStatus = "Slightly Behind";
                            BigDecimal dailyNeeded = remainingDays > 0
                                    ? savingGoal.getTargetAmount().subtract(savingGoal.getSavedAmount())
                                    .divide(BigDecimal.valueOf(remainingDays), 2, RoundingMode.HALF_UP)
                                    : BigDecimal.ZERO;
                            notification = String.format("Attention needed: Your %s saving goal is slightly behind (%.2f%% vs %.2f%% expected). Try saving %.2f/day to catch up.",
                                    savingGoal.getCategory().getName(), savedPercentage, expectedPercentage, dailyNeeded);
                        } else {
                            progressStatus = "At Risk";
                            BigDecimal dailyNeeded = remainingDays > 0
                                    ? savingGoal.getTargetAmount().subtract(savingGoal.getSavedAmount())
                                    .divide(BigDecimal.valueOf(remainingDays), 2, RoundingMode.HALF_UP)
                                    : BigDecimal.ZERO;
                            notification = String.format("Action required: Your %s saving goal is significantly behind (%.2f%% vs %.2f%% expected). You need to save %.2f/day to reach your target.",
                                    savingGoal.getCategory().getName(), savedPercentage, expectedPercentage, dailyNeeded);
                        }
                    }

                    dto.setProgressStatus(progressStatus);
                    dto.setNotification(notification);
                    return dto;
                })
                .collect(Collectors.toList());
    }
}