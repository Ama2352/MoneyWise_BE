package JavaProject.MoneyManagement_BE_SE330.models.dtos.report;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ReportInfoDTO {

    @NotNull(message = "startDate is required")
    private LocalDate startDate;

    private LocalDate endDate; // input this field = null if type include *summary

    @NotNull(message = "type is required")
    private String type;

    @NotNull(message = "format is required")
    private String format;

    private String currency; // if not provided, default is VND
}