package JavaProject.MoneyManagement_BE_SE330.services.impls;

import JavaProject.MoneyManagement_BE_SE330.helper.ApplicationMapper;
import JavaProject.MoneyManagement_BE_SE330.helper.HelperFunctions;
import JavaProject.MoneyManagement_BE_SE330.helper.ResourceNotFoundException;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.wallet.CreateWalletDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.wallet.UpdateWalletDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.wallet.WalletDTO;
import JavaProject.MoneyManagement_BE_SE330.models.entities.User;
import JavaProject.MoneyManagement_BE_SE330.models.entities.Wallet;
import JavaProject.MoneyManagement_BE_SE330.repositories.UserRepository;
import JavaProject.MoneyManagement_BE_SE330.repositories.WalletRepository;
import JavaProject.MoneyManagement_BE_SE330.services.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final ApplicationMapper applicationMapper;

    @Override
    public List<WalletDTO> getAllWallets() {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        return walletRepository.findAllByUser(currentUser)
                .stream()
                .map(applicationMapper::toWalletDTO)
                .toList();
    }

    @Override
    public WalletDTO getWalletById(UUID walletId) {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        Wallet wallet = walletRepository.findByWalletIDAndUser(walletId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        return applicationMapper.toWalletDTO(wallet);
    }

    @Override
    public WalletDTO createWallet(CreateWalletDTO model) {
        Wallet wallet = applicationMapper.toWalletEntity(model);
        wallet.setUser(HelperFunctions.getCurrentUser(userRepository));
        walletRepository.save(wallet);
        return applicationMapper.toWalletDTO(wallet);
    }

    @Override
    public WalletDTO updateWallet(UpdateWalletDTO model) {
        Wallet wallet = walletRepository.findById(model.getWalletID())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        wallet.setUser(HelperFunctions.getCurrentUser(userRepository));

        wallet.setWalletName(model.getWalletName());
        wallet.setBalance(model.getBalance());

        walletRepository.save(wallet);
        return applicationMapper.toWalletDTO(wallet);
    }

    @Override
    public UUID deleteWalletById(UUID walletId) {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        Wallet wallet = walletRepository.findByWalletIDAndUser(walletId, currentUser)
                        .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        walletRepository.delete(wallet);
        return walletId;
    }
}
