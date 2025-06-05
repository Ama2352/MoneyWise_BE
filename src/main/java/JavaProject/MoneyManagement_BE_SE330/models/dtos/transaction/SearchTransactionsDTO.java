package JavaProject.MoneyManagement_BE_SE330.models.dtos.transaction;

import jakarta.validation.constraints.NotNull;
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

    private String category;
    private String amountRange;
    private String keywords;
    private String timeRange;
    private String dayOfWeek;
}
