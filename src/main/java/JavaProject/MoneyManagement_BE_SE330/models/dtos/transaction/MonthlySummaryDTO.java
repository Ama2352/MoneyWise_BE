package JavaProject.MoneyManagement_BE_SE330.models.dtos.transaction;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySummaryDTO {

    private int month;

    private int year;

    private String monthName = "";

    private List<MonthlyDetailDTO> monthlyDetails;

    private BigDecimal totalIncome;

    private BigDecimal totalExpenses;

    private BigDecimal netCashFlow;

    private List<TransactionDetailDTO> transactions = new ArrayList<>();

    private Map<Integer, BigDecimal> dailyTotals = new HashMap<>();

    private Map<String, BigDecimal> categoryTotals = new HashMap<>();

}
