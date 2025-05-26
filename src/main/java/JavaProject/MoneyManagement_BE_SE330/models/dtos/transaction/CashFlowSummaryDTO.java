package JavaProject.MoneyManagement_BE_SE330.models.dtos.transaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowSummaryDTO {
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
}