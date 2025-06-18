package JavaProject.MoneyWise.models.dtos.report;

import jakarta.validation.constraints.NotNull;
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