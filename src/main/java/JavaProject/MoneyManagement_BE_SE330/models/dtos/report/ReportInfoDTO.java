package JavaProject.MoneyManagement_BE_SE330.models.dtos.report;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReportInfoDTO {

    @NotNull(message = "startDate is required")
    private LocalDate startDate;

    private LocalDate endDate;

    @NotNull(message = "type is required")
    private String type;

    @NotNull(message = "format is required")
    private String format;

    private String currency;

    @AssertTrue(message = "startDate must be before endDate")
    public boolean isStartDateBeforeEndDate() {
        // Nếu endDate là null thì coi như không giới hạn, hợp lệ
        if (startDate == null || endDate == null) return true;
        return startDate.isBefore(endDate);
    }
}
