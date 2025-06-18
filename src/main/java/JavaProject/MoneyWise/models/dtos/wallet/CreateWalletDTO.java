package JavaProject.MoneyWise.models.dtos.wallet;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateWalletDTO {

    @NotNull(message = "WalletName must not be null")
    private String walletName;

    @NotNull(message = "WalletBalance must not be null")
    private BigDecimal balance;
}
