package JavaProject.MoneyWise.services;

import JavaProject.MoneyWise.models.dtos.savingGoal.CreateSavingGoalDTO;
import JavaProject.MoneyWise.models.dtos.savingGoal.SavingGoalDTO;
import JavaProject.MoneyWise.models.dtos.savingGoal.SavingGoalProgressDTO;
import JavaProject.MoneyWise.models.dtos.savingGoal.UpdateSavingGoalDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface SavingGoalService {
    @Transactional
    SavingGoalDTO createSavingGoal(CreateSavingGoalDTO model);

    @Transactional(readOnly = true)
    List<SavingGoalDTO> getAllSavingGoals();

    @Transactional(readOnly = true)
    SavingGoalDTO getSavingGoalById(UUID id);

    @Transactional
    SavingGoalDTO updateSavingGoal(UpdateSavingGoalDTO model);

    @Transactional
    UUID deleteSavingGoal(UUID id);

    @Transactional(readOnly = true)
    List<SavingGoalProgressDTO> getSavingGoalProgressAndAlerts();
}
