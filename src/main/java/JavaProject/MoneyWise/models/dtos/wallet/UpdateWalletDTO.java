package JavaProject.MoneyWise.models.dtos.wallet;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateWalletDTO {
    @NotNull(message = "WalletId must not be null")
    private UUID walletId;

    @NotNull(message = "WalletName must not be null")
    private String walletName;

    @NotNull(message = "WalletBalance must not be null")
    private BigDecimal balance;
}
