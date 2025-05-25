package JavaProject.MoneyManagement_BE_SE330.models.dtos.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportInfoDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    private String type;
    private String format;
}