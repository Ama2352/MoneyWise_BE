package JavaProject.MoneyWise.models.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "budgets")
@ToString(exclude = { "category", "wallet" })
public class Budget {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID budgetId;

    @ManyToOne
    @JoinColumn(name = "categoryId", nullable = false)
    private Category category;
    @ManyToOne
    @JoinColumn(name = "walletId", nullable = false)
    private Wallet wallet;

    private String description;

    @Column(nullable = false)
    private BigDecimal limitAmount;

    @Column(nullable = false)
    private BigDecimal currentSpending = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}