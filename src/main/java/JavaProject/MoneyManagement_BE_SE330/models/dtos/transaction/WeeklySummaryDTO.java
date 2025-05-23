package JavaProject.MoneyManagement_BE_SE330.models.dtos.transaction;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklySummaryDTO {
    private List<WeeklyDetailDTO> weeklyDetails;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
}
