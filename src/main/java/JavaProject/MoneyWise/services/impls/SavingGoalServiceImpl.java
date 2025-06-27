package JavaProject.MoneyWise.services.impls;

import JavaProject.MoneyWise.helper.ApplicationMapper;
import JavaProject.MoneyWise.helper.HelperFunctions;
import JavaProject.MoneyWise.helper.ResourceNotFoundException;
import JavaProject.MoneyWise.models.dtos.savingGoal.*;
import JavaProject.MoneyWise.models.entities.*;
import JavaProject.MoneyWise.repositories.CategoryRepository;
import JavaProject.MoneyWise.repositories.SavingGoalRepository;
import JavaProject.MoneyWise.repositories.UserRepository;
import JavaProject.MoneyWise.repositories.WalletRepository;
import JavaProject.MoneyWise.services.SavingGoalService;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SavingGoalServiceImpl implements SavingGoalService {
    private final SavingGoalRepository savingGoalRepository;
    private final CategoryRepository categoryRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final ApplicationMapper applicationMapper;
    private final MessageSource messageSource;

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
    public List<SavingGoalProgressDTO> searchSavingGoals(SearchSavingGoalsDTO filter) {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        LocalDateTime startDateTime = filter.getStartDate() != null ? filter.getStartDate().atStartOfDay() : null;
        LocalDateTime endDateTime = filter.getEndDate() != null ? filter.getEndDate().atTime(LocalTime.MAX) : null;

        List<SavingGoalProgressDTO> savingGoalProgress = getSavingGoalProgressAndAlerts();

        Map<UUID, SavingGoalProgressDTO> progressMap = savingGoalProgress.stream()
                .collect(Collectors.toMap(SavingGoalProgressDTO::getSavingGoalId, dto -> dto));

        List<SavingGoal> savingGoals = savingGoalRepository
                .findByWalletUser(currentUser)
                .stream()
                .filter(savingGoal -> {
                    if (startDateTime != null && endDateTime != null) {
                        return !savingGoal.getStartDate().isAfter(endDateTime) &&
                                !savingGoal.getEndDate().isBefore(startDateTime);
                    } else if (startDateTime != null) {
                        return !savingGoal.getStartDate().isBefore(startDateTime);
                    } else if (endDateTime != null) {
                        return !savingGoal.getEndDate().isAfter(endDateTime);
                    }
                    return true;
                })
                .filter(savingGoal -> filter.getKeywords() == null ||
                        (savingGoal.getDescription() != null &&
                                savingGoal.getDescription().toLowerCase().contains(filter.getKeywords().toLowerCase())))
                .filter(savingGoal -> filter.getCategoryName() == null ||
                        savingGoal.getCategory().getName().equalsIgnoreCase(filter.getCategoryName()))
                .filter(savingGoal -> filter.getWalletName() == null ||
                        savingGoal.getWallet().getWalletName().equalsIgnoreCase(filter.getWalletName()))
                .filter(savingGoal -> filter.getTargetAmount() == null ||
                        savingGoal.getTargetAmount().compareTo(filter.getTargetAmount()) == 0)
                .sorted(Comparator.comparing(SavingGoal::getCreatedAt).reversed())
                .toList();

        return savingGoals.stream()
                .map(savingGoal -> progressMap.get(savingGoal.getSavingGoalId()))
                .filter(Objects::nonNull)
                .toList();
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

                    // Calculate expected savings
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
                    String progressStatus = determineProgressStatus(
                            currentDateTime,
                            savingGoal,
                            savedPercentage,
                            expectedSavedAmount,
                            remainingDays
                    );

                    String notification = generateNotification(
                            progressStatus,
                            savingGoal,
                            savedPercentage,
                            expectedPercentage,
                            remainingDays,
                            expectedSavedAmount
                    );

                    // Localize the progress status
                    String localizedProgressStatus = messageSource.getMessage(
                            "saving.goal.status." + progressStatus.toLowerCase().replace(" ", "."),
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
            SavingGoal savingGoal,
            BigDecimal savedPercentage,
            BigDecimal expectedSavedAmount,
            long remainingDays
    ) {
        if (currentDateTime.isBefore(savingGoal.getStartDate())) {
            return "Not Started";
        }

        if (currentDateTime.isAfter(savingGoal.getEndDate())) {
            if (savedPercentage.compareTo(BigDecimal.valueOf(100)) >= 0) {
                return "Achieved";
            } else if (savedPercentage.compareTo(BigDecimal.valueOf(75)) >= 0) {
                return "Partially Achieved";
            } else {
                return "Missed Target";
            }
        }

        if (savingGoal.getSavedAmount().compareTo(savingGoal.getTargetAmount()) >= 0) {
            return "Achieved Early";
        }

        BigDecimal progressRatio = expectedSavedAmount.compareTo(BigDecimal.ZERO) > 0
                ? savingGoal.getSavedAmount().divide(expectedSavedAmount, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        if (progressRatio.compareTo(BigDecimal.valueOf(1.2)) > 0) {
            return "Ahead";
        } else if (progressRatio.compareTo(BigDecimal.valueOf(0.8)) >= 0) {
            return "On Track";
        } else if (progressRatio.compareTo(BigDecimal.valueOf(0.6)) >= 0) {
            return "Slightly Behind";
        } else {
            return "At Risk";
        }
    }

    private String generateNotification(
            String progressStatus,
            SavingGoal savingGoal,
            BigDecimal savedPercentage,
            BigDecimal expectedPercentage,
            long remainingDays,
            BigDecimal expectedSavedAmount
    ) {
        String categoryName = savingGoal.getCategory().getName();
        String startDate = savingGoal.getStartDate().toLocalDate().toString();
        String endDate = savingGoal.getEndDate().toLocalDate().toString();
        BigDecimal targetAmount = savingGoal.getTargetAmount();
        BigDecimal savedAmount = savingGoal.getSavedAmount();

        BigDecimal dailyNeeded = remainingDays > 0
                ? targetAmount.subtract(savedAmount)
                .divide(BigDecimal.valueOf(remainingDays), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        switch (progressStatus) {
            case "Not Started":
                return messageSource.getMessage(
                        "saving.goal.not.started",
                        new Object[]{categoryName, startDate, targetAmount},
                        LocaleContextHolder.getLocale()
                );

            case "Achieved":
                return messageSource.getMessage(
                        "saving.goal.achieved",
                        new Object[]{categoryName, savedPercentage},
                        LocaleContextHolder.getLocale()
                );

            case "Partially Achieved":
                return messageSource.getMessage(
                        "saving.goal.partially.achieved",
                        new Object[]{categoryName, savedPercentage},
                        LocaleContextHolder.getLocale()
                );

            case "Missed Target":
                return messageSource.getMessage(
                        "saving.goal.missed.target",
                        new Object[]{categoryName, savedPercentage},
                        LocaleContextHolder.getLocale()
                );

            case "Achieved Early":
                return messageSource.getMessage(
                        "saving.goal.achieved.early",
                        new Object[]{categoryName, savedPercentage, remainingDays},
                        LocaleContextHolder.getLocale()
                );

            case "Ahead":
                return messageSource.getMessage(
                        "saving.goal.ahead",
                        new Object[]{categoryName, savedPercentage, expectedPercentage},
                        LocaleContextHolder.getLocale()
                );

            case "On Track":
                return messageSource.getMessage(
                        "saving.goal.on.track",
                        new Object[]{categoryName, savedPercentage, targetAmount.subtract(savedAmount), endDate},
                        LocaleContextHolder.getLocale()
                );

            case "Slightly Behind":
                return messageSource.getMessage(
                        "saving.goal.slightly.behind",
                        new Object[]{categoryName, savedPercentage, expectedPercentage, dailyNeeded},
                        LocaleContextHolder.getLocale()
                );

            case "At Risk":
                return messageSource.getMessage(
                        "saving.goal.at.risk",
                        new Object[]{categoryName, savedPercentage, expectedPercentage, dailyNeeded},
                        LocaleContextHolder.getLocale()
                );

            default:
                return "";
        }
    }
}