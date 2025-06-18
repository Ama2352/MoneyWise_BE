package JavaProject.MoneyWise.models.dtos.transaction;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreateTransactionDTO {
    @NotNull(message = "CategoryID must not be null")
    private UUID categoryId;

    @NotNull(message = "Amount must not be null")
    private BigDecimal amount;

    private String description = "No Description";

    @NotNull(message = "TransactionDate must not be null")
    private LocalDateTime transactionDate;
    @NotNull(message = "WalletID must not be null")
    private UUID walletId;

    @Pattern(regexp = "Income|Expense", flags = Pattern.Flag.CASE_INSENSITIVE)
    @NotNull(message = "Type must not be null")
    private String type;
}
