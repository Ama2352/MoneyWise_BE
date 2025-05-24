package JavaProject.MoneyManagement_BE_SE330.models.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "messages")
@ToString(exclude = {"sender", "receiver"})
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID messageID;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
    }
}
