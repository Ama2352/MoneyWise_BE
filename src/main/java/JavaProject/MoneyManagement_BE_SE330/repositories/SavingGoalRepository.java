package JavaProject.MoneyManagement_BE_SE330.repositories;

import JavaProject.MoneyManagement_BE_SE330.models.entities.Category;
import JavaProject.MoneyManagement_BE_SE330.models.entities.SavingGoal;
import JavaProject.MoneyManagement_BE_SE330.models.entities.User;
import JavaProject.MoneyManagement_BE_SE330.models.entities.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public interface SavingGoalRepository extends JpaRepository<SavingGoal, UUID> {
    List<SavingGoal> findByCategoryAndWalletAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Category category, Wallet wallet, LocalDateTime startDate, LocalDateTime endDate);

    @Modifying
    @Transactional
    @Query("UPDATE SavingGoal usg SET usg.savedAmount = usg.savedAmount + :amount " +
            "WHERE usg.category = :category AND usg.wallet = :wallet " +
            "AND :transactionDate BETWEEN usg.startDate AND usg.endDate")
    void updateSavedAmount(Category category, Wallet wallet, BigDecimal amount, LocalDateTime transactionDate);

    List<SavingGoal> findByCategoryAndWalletUserAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Category category, User user, LocalDateTime startDate, LocalDateTime endDate);

    List<SavingGoal> findByWalletUser(User currentUser);
}