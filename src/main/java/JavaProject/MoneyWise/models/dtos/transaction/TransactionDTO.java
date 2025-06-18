package JavaProject.MoneyWise.models.dtos.transaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TransactionDTO {
    private UUID transactionId;
    private UUID categoryId;
    private BigDecimal amount;
    private String description; // nullable by default in Java
    private LocalDateTime transactionDate;
    private String type;
    private UUID walletId;
}