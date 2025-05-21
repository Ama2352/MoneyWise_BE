package JavaProject.MoneyManagement_BE_SE330.models.dtos.transaction;

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
    private UUID transactionID;
    private UUID categoryID;
    private BigDecimal amount;
    private String description; // nullable by default in Java
    private LocalDateTime transactionDate;
    private String type;
    private UUID walletID;
}