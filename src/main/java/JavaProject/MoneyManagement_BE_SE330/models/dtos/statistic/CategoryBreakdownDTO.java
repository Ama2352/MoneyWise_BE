package JavaProject.MoneyManagement_BE_SE330.models.dtos.statistic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryBreakdownDTO {
    private String category = "";
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal incomePercentage;
    private BigDecimal expensePercentage;
    private BigDecimal budgetLimit;
    private BigDecimal budgetCurrentSpending;
    private BigDecimal goalTarget;
    private BigDecimal goalSaved;
}