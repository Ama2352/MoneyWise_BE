package JavaProject.MoneyManagement_BE_SE330.repositories;

import JavaProject.MoneyManagement_BE_SE330.models.entities.User;
import JavaProject.MoneyManagement_BE_SE330.models.entities.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    List<Wallet> findAllByUser(User user);
    Optional<Wallet> findByWalletIdAndUser(UUID walletId, User user);
    boolean existsByUser(User user);
}
