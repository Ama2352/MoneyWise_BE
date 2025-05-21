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

    private LocalDate startDate;

    private LocalDate endDate;

    private int weekNumber;

    private int year;

    private List<WeeklyDetailDTO> weeklyDetails;

    private BigDecimal totalIncome;

    private BigDecimal totalExpenses;

    private BigDecimal netCashFlow;

    private List<TransactionDetailDTO> transactions = new ArrayList<>();

    private Map<String, BigDecimal> dailyTotals = new HashMap<>();

    private Map<String, BigDecimal> dailyIncomeTotals = new HashMap<>();

    private Map<String, BigDecimal> dailyExpenseTotals = new HashMap<>();
}
