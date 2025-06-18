package JavaProject.MoneyWise.services;

import JavaProject.MoneyWise.models.dtos.wallet.CreateWalletDTO;
import JavaProject.MoneyWise.models.dtos.wallet.UpdateWalletDTO;
import JavaProject.MoneyWise.models.dtos.wallet.WalletDTO;

import java.util.List;
import java.util.UUID;

public interface WalletService {
    List<WalletDTO> getAllWallets();
    List<WalletDTO> getAllWallets(String acceptLanguage);
    WalletDTO getWalletById(UUID walletId);
    WalletDTO createWallet(CreateWalletDTO model);
    WalletDTO updateWallet(UpdateWalletDTO model);
    UUID deleteWalletById(UUID walletId);
}
