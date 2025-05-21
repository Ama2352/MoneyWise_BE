package JavaProject.MoneyManagement_BE_SE330.models.dtos.transaction;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailySummaryDTO {

    private LocalDate date;

    private String dayOfWeek = "";

    private String month = "";

    private List<DailyDetailDTO> dailyDetails;

    private BigDecimal totalIncome;

    private BigDecimal totalExpenses;

    private List<TransactionDetailDTO> transactions = List.of();

}