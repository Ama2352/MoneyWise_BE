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
public class YearlySummaryDTO {
    private List<YearlyDetailDTO> yearlyDetails;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
}
