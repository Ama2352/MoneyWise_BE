package JavaProject.MoneyWise.repositories;

import JavaProject.MoneyWise.models.entities.Transaction;
import JavaProject.MoneyWise.models.entities.User;
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

    @Query("SELECT t FROM Transaction t WHERE t.wallet.user = :user AND t.category.user = :user")
    List<Transaction> findAllByWalletUserAndCategoryUser(@Param("user") User user);

    List<Transaction> findByWalletWalletIdInAndTransactionDateBetween(List<UUID> walletIds, LocalDateTime startOfDay, LocalDateTime endOfDay);

    List<Transaction> findByWalletWalletIdInAndTransactionDateAfter(List<UUID> userWalletIds, LocalDateTime startDateTime);

    List<Transaction> findByWalletWalletIdInAndTransactionDateBefore(List<UUID> userWalletIds, LocalDateTime endDateTime);
}
