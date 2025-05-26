package JavaProject.MoneyManagement_BE_SE330.models.dtos.savingGoal;

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
public class UpdateSavingGoalDTO {

    @NotNull(message = "SavingGoalId must not be null")
    private UUID savingGoalId;

    @NotNull(message = "EndDate must not be null")
    private LocalDateTime endDate;

    @NotNull(message = "StartDate must not be null")
    private LocalDateTime startDate;

    @NotNull(message = "TargetAmount must not be null")
    private BigDecimal targetAmount;

    @NotNull(message = "Description must not be null")
    private String description;

    @NotNull(message = "CategoryID must not be null")
    private UUID categoryID;

    @NotNull(message = "WalletID must not be null")
    private UUID walletID;
}
