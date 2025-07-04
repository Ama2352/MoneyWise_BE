package JavaProject.MoneyWise.models.dtos.transaction;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetTransactionsByDateRangeDTO {

    private LocalDate startDate;
    private LocalDate endDate;

    @Pattern(regexp = "Income|Expense", flags = Pattern.Flag.CASE_INSENSITIVE)
    private String type;

    private String category;
    private String timeRange;
    private String dayOfWeek;
}
