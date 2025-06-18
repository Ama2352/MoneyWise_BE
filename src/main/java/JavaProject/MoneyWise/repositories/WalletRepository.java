package JavaProject.MoneyWise.repositories;

import JavaProject.MoneyWise.models.entities.User;
import JavaProject.MoneyWise.models.entities.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    List<Wallet> findAllByUser(User user);
    Optional<Wallet> findByWalletIdAndUser(UUID walletId, User user);
    boolean existsByUser(User user);

    @Modifying
    @Transactional
    @Query("UPDATE Wallet w SET w.balance = w.balance + :amount WHERE w.walletId = :walletId")
    void updateBalance(UUID walletId, BigDecimal amount);
}
