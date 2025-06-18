package JavaProject.MoneyWise.models.dtos.statistic;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailySummaryDTO {
    private List<DailyDetailDTO> dailyDetails;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
}