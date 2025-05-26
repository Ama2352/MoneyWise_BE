package JavaProject.MoneyManagement_BE_SE330.models.dtos.budget;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateBudgetDTO {

    @NotNull(message = "EndDate must not be null")
    private LocalDateTime endDate;

    @NotNull(message = "StartDate must not be null")
    private LocalDateTime startDate;

    @NotNull(message = "LimitAmount must not be null")
    private BigDecimal limitAmount;

    @NotNull(message = "Description must not be null")
    private String description;

    @NotNull(message = "CategoryID must not be null")
    private UUID categoryID;

    @NotNull(message = "WalletID must not be null")
    private UUID walletID;

    @NotNull(message = "BudgetId must not be null")
    private UUID budgetId;
}
