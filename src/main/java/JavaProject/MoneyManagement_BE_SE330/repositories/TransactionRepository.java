package JavaProject.MoneyManagement_BE_SE330.repositories;

import JavaProject.MoneyManagement_BE_SE330.models.entities.Transaction;
import JavaProject.MoneyManagement_BE_SE330.models.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByWalletWalletId(UUID walletId);

    // Find all transactions for every wallet that belongs to the given user.
    @Query("SELECT t FROM Transaction t WHERE t.wallet.user = :user")
    List<Transaction> findAllByWalletUser(@Param("user") User user);

    List<Transaction> findByWalletWalletIdInAndTransactionDateBetween(List<UUID> walletIds, LocalDateTime startOfDay, LocalDateTime endOfDay);
}
