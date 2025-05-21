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
    @NotNull(message = "StartDate must not be null")
    private LocalDate startDate;

    @NotNull(message = "EndDate must not be null")
    private LocalDate endDate;

    @Pattern(regexp = "income|expense", message = "Type must be either 'income' or 'expense'")
    private String type;

    private String category;
    private String amountRange;
    private String keywords;
    private String timeRange;
    private String dayOfWeek;
}
