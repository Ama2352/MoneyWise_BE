package JavaProject.MoneyWise.services;

import JavaProject.MoneyWise.models.dtos.budget.*;
import JavaProject.MoneyWise.models.entities.Budget;
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
    List<BudgetProgressDTO> searchBudgets(SearchBudgetsDTO filter, String acceptLanguage);

    @Transactional(readOnly = true)
    List<BudgetProgressDTO> getBudgetProgressAndAlerts(String acceptLanguage, String currency);
}
