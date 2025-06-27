package JavaProject.MoneyWise.repositories;

import JavaProject.MoneyWise.models.entities.Category;
import JavaProject.MoneyWise.models.entities.Budget;
import JavaProject.MoneyWise.models.entities.User;
import JavaProject.MoneyWise.models.entities.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    List<Budget> findByCategoryAndWalletAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Category category, Wallet wallet, LocalDateTime startDate, LocalDateTime endDate);

    @Modifying
    @Transactional
    @Query("UPDATE Budget ub SET ub.currentSpending = ub.currentSpending + :amount " +
            "WHERE ub.category = :category AND ub.wallet = :wallet " +
            "AND :transactionDate BETWEEN ub.startDate AND ub.endDate")
    void updateCurrentSpending(Category category, Wallet wallet, BigDecimal amount, LocalDateTime transactionDate);

    List<Budget> findByCategoryAndWalletUserAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Category category, User user, LocalDateTime startDate, LocalDateTime endDate);

    List<Budget> findByWalletUser(User user);

    List<Budget> findByWalletAndStartDateLessThanEqualAndEndDateGreaterThanEqual(Wallet wallet, LocalDateTime localDateTime, LocalDateTime localDateTime1);
}
