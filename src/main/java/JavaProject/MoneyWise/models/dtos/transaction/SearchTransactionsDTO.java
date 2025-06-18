package JavaProject.MoneyWise.models.dtos.transaction;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchTransactionsDTO {

    private LocalDate startDate;
    private LocalDate endDate;

    @Pattern(regexp = "Income|Expense", flags = Pattern.Flag.CASE_INSENSITIVE)
    private String type;

    private String categoryName;
    private String walletName;
    private String amountRange;
    private String keywords;
    private String timeRange;
    private String dayOfWeek;
}
