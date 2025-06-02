package JavaProject.MoneyManagement_BE_SE330.services.impls;

import JavaProject.MoneyManagement_BE_SE330.helper.ApplicationMapper;
import JavaProject.MoneyManagement_BE_SE330.helper.HelperFunctions;
import JavaProject.MoneyManagement_BE_SE330.helper.LocalizationUtils;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


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
    public List<WalletDTO> getAllWallets(String acceptLanguage) {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        List<Wallet> userWallets = walletRepository.findAllByUser(currentUser);

        // If user has no wallets yet, create localized seed wallets
        if(userWallets.isEmpty()) {
            return createLocalizedSeedWallets(currentUser, acceptLanguage);
        }

        // Return existing wallets (they were created with localized names)
        return userWallets.stream()
                .map(applicationMapper::toWalletDTO)
                .toList();
    }

    private List<WalletDTO> createLocalizedSeedWallets(User user, String acceptLanguage) {
        boolean isVietnamese = LocalizationUtils.isVietnamese(acceptLanguage);
        List<String> localizedNames = LocalizationUtils.getLocalizedWallets(isVietnamese);

        List<BigDecimal> seedBalances = new ArrayList<>();
        seedBalances.add(new BigDecimal("5000000"));
        seedBalances.add(new BigDecimal("10000000"));
        seedBalances.add(new BigDecimal("1000000"));

        List<Wallet> seedWallets = IntStream.range(0, localizedNames.size())
                .mapToObj(i -> new Wallet(localizedNames.get(i), seedBalances.get(i), user))
                .toList();

        walletRepository.saveAll(seedWallets);
        return seedWallets.stream()
                .map(applicationMapper::toWalletDTO)
                .toList();
    }

    @Override
    public WalletDTO getWalletById(UUID walletId) {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        Wallet wallet = walletRepository.findByWalletIdAndUser(walletId, currentUser)
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
        Wallet wallet = walletRepository.findByWalletIdAndUser(walletId, currentUser)
                        .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        walletRepository.delete(wallet);
        return walletId;
    }
}
