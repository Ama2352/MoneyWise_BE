package JavaProject.MoneyManagement_BE_SE330.services.impls;

import JavaProject.MoneyManagement_BE_SE330.helper.ApplicationMapper;
import JavaProject.MoneyManagement_BE_SE330.helper.HelperFunctions;
import JavaProject.MoneyManagement_BE_SE330.helper.ResourceNotFoundException;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.savingGoal.CreateSavingGoalDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.savingGoal.SavingGoalDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.savingGoal.UpdateSavingGoalDTO;
import JavaProject.MoneyManagement_BE_SE330.models.entities.Category;
import JavaProject.MoneyManagement_BE_SE330.models.entities.SavingGoal;
import JavaProject.MoneyManagement_BE_SE330.models.entities.User;
import JavaProject.MoneyManagement_BE_SE330.models.entities.Wallet;
import JavaProject.MoneyManagement_BE_SE330.repositories.CategoryRepository;
import JavaProject.MoneyManagement_BE_SE330.repositories.SavingGoalRepository;
import JavaProject.MoneyManagement_BE_SE330.repositories.UserRepository;
import JavaProject.MoneyManagement_BE_SE330.repositories.WalletRepository;
import JavaProject.MoneyManagement_BE_SE330.services.SavingGoalService;
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
    public List<SavingGoalDTO> getSavingGoalProgressAndAlerts() {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        LocalDateTime currentDateTime = LocalDateTime.now(); // May 26, 2025, 8:38 PM +07

        return savingGoalRepository.findByWalletUser(currentUser).stream()
                .map(savingGoal -> {
                    SavingGoalDTO dto = applicationMapper.toSavingGoalDTO(savingGoal);

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

                    // Determine progress status and notification
                    String progressStatus;
                    String notification = null;
                    if (currentDateTime.isBefore(savingGoal.getStartDate())) {
                        progressStatus = "Pending";
                    } else if (savingGoal.getSavedAmount().compareTo(savingGoal.getTargetAmount()) >= 0 || currentDateTime.isAfter(endDate)) {
                        progressStatus = "Complete";
                        if (savedPercentage.compareTo(BigDecimal.valueOf(100)) >= 0) {
                            notification = String.format("Congratulations: Saving goal for category %s has reached or exceeded the target at %.2f%%!",
                                    savingGoal.getCategory().getName(), savedPercentage);
                        } else if (currentDateTime.isAfter(endDate)) {
                            notification = String.format("Completed: Saving goal for category %s ended with %.2f%% of the target.",
                                    savingGoal.getCategory().getName(), savedPercentage);
                        }
                    } else {
                        boolean pastMidpoint = elapsedDays > totalDays / 2;
                        if (!pastMidpoint) {
                            progressStatus = "Safe";
                        } else {
                            BigDecimal expectedThreshold = expectedSavedAmount.multiply(BigDecimal.valueOf(0.8)); // 80% of expected
                            if (savingGoal.getSavedAmount().compareTo(expectedThreshold) >= 0) {
                                progressStatus = "Safe";
                            } else {
                                progressStatus = "Warning";
                                BigDecimal shortfall = expectedSavedAmount.subtract(savingGoal.getSavedAmount());
                                notification = String.format("Warning: Saving goal for category %s is behind schedule. You need $%.2f more to meet the expected progress (%.2f%%) by %s.",
                                        savingGoal.getCategory().getName(), shortfall, expectedPercentage,
                                        endDate.toLocalDate());
                            }
                        }
                    }
                    dto.setProgressStatus(progressStatus);
                    dto.setNotification(notification);

                    return dto;
                })
                .collect(Collectors.toList());
    }
}