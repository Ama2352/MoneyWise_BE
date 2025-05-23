package JavaProject.MoneyManagement_BE_SE330.models.dtos.transaction;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySummaryDTO {
    private List<MonthlyDetailDTO> monthlyDetails;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
}
