package JavaProject.MoneyWise.models.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "wallets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {

    @Id
    private UUID walletId;

    @Column(nullable = false)
    private String walletName;

    @Column(nullable = false)
    private BigDecimal balance;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL)
    private List<Transaction> transactions;

    public Wallet(String walletName, BigDecimal balance, User user) {
        this.walletName = walletName;
        this.balance = balance;
        this.user = user;
    }

    @PrePersist
    protected void onCreate() {
        if(walletId == null) {
            walletId = UUID.randomUUID();
        }
    }
}
