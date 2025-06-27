package JavaProject.MoneyWise.models.dtos.statistic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletBreakdownDTO {
    private String wallet = "";
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal incomePercentage;
    private BigDecimal expensePercentage;
    private BigDecimal budgetLimit;
    private BigDecimal budgetCurrentSpending;
    private BigDecimal goalTarget;
    private BigDecimal goalSaved;
}
