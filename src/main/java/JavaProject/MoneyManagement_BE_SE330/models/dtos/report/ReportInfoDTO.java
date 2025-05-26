package JavaProject.MoneyManagement_BE_SE330.models.dtos.report;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import jakarta.validation.constraints.AssertTrue;

@Data
public class ReportInfoDTO {

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    private String type;
    private String format;
    private String currency;

    @AssertTrue(message = "startDate must be before endDate")
    public boolean isStartDateBeforeEndDate() {
        if (startDate == null || endDate == null) return true; // avoid double error when @NotNull fails
        return startDate.isBefore(endDate);
    }
}
