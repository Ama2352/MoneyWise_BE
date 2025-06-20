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
@ToString(exclude = { "category", "wallet" })
@Table(name = "savingGoals")
public class SavingGoal {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID savingGoalId;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    private String description;

    @Column(nullable = false)
    private BigDecimal targetAmount;

    @Column(nullable = false)
    private BigDecimal savedAmount = BigDecimal.ZERO;

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