package JavaProject.MoneyManagement_BE_SE330.models.dtos.statistic;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YearlySummaryDTO {
    private List<YearlyDetailDTO> yearlyDetails;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
}
