package JavaProject.MoneyWise.models.dtos.statistic;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YearlyDetailDTO {

    private String year;

    private BigDecimal income;

    private BigDecimal expense;

}
