package JavaProject.MoneyWise.models.dtos.statistic;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklySummaryDTO {
    private List<WeeklyDetailDTO> weeklyDetails;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
}
