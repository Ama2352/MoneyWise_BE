package JavaProject.MoneyWise.models.dtos.budget;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBudgetDTO {

    @NotNull(message = "EndDate must not be null")
    private LocalDateTime endDate;

    @NotNull(message = "StartDate must not be null")
    private LocalDateTime startDate;

    @NotNull(message = "LimitAmount must not be null")
    private BigDecimal limitAmount;

    @NotNull(message = "Description must not be null")
    private String description;

    @NotNull(message = "CategoryId must not be null")
    private UUID categoryId;

    @NotNull(message = "WalletId must not be null")
    private UUID walletId;
}
