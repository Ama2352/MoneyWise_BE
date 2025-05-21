package JavaProject.MoneyManagement_BE_SE330.services;

import JavaProject.MoneyManagement_BE_SE330.models.dtos.wallet.CreateWalletDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.wallet.UpdateWalletDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.wallet.WalletDTO;

import java.util.List;
import java.util.UUID;

public interface WalletService {
    List<WalletDTO> getAllWallets();
    WalletDTO getWalletById(UUID walletId);
    WalletDTO createWallet(CreateWalletDTO model);
    WalletDTO updateWallet(UpdateWalletDTO model);
    UUID deleteWalletById(UUID walletId);
}
