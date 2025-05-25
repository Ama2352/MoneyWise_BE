package JavaProject.MoneyManagement_BE_SE330.models.dtos.budget;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BudgetDTO {
    private UUID budgetId;
    private UUID categoryId;
    private UUID walletId;
    private String description;
    private BigDecimal limitAmount;
    private BigDecimal currentSpending;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
}
