package JavaProject.MoneyWise.models.dtos.savingGoal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchSavingGoalsDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    private String keywords;
    private String categoryName;
    private String walletName;
    private BigDecimal targetAmount;
}
