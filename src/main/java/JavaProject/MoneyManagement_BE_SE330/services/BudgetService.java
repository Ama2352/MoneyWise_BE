package JavaProject.MoneyManagement_BE_SE330.services;

import JavaProject.MoneyManagement_BE_SE330.models.dtos.budget.BudgetDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.budget.BudgetProgressDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.budget.CreateBudgetDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.budget.UpdateBudgetDTO;
import JavaProject.MoneyManagement_BE_SE330.models.entities.Budget;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface BudgetService {
    public BudgetDTO createBudget(CreateBudgetDTO model);

    @Transactional(readOnly = true)
    List<Budget> getAllBudgets();

    @Transactional(readOnly = true)
    BudgetDTO getBudgetById(UUID id);

    @Transactional
    BudgetDTO updateBudget(UpdateBudgetDTO model);

    @Transactional
    UUID deleteBudget(UUID id);

    @Transactional(readOnly = true)
    List<BudgetProgressDTO> getBudgetProgressAndAlerts();
}
