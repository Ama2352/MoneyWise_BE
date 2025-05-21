package JavaProject.MoneyManagement_BE_SE330.models.dtos.wallet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class WalletDTO {
    private UUID walletId;
    private String walletName;
    private BigDecimal balance;
}
