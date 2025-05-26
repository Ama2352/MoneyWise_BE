package JavaProject.MoneyManagement_BE_SE330.models.dtos.report;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ReportInfoDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    private String type;
    private String format;
    private String currency;
}