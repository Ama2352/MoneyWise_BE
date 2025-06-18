package JavaProject.MoneyWise.models.dtos.savingGoal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavingGoalProgressDTO {
    private UUID savingGoalId;
    private UUID categoryId;
    private UUID walletId;
    private String description;
    private BigDecimal targetAmount;
    private BigDecimal savedAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
    private BigDecimal savedPercentage = BigDecimal.ZERO;
    private String progressStatus = "Safe";
    private String notification;
}
