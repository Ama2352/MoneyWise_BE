package JavaProject.MoneyWise.models.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_friends")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"user","friend"})
public class UserFriend {

    @EmbeddedId
    UserFriendId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user; // The user who sent the friend request

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("friendId")
    @JoinColumn(name = "friend_id")
    private User friend; // The user who received the friend request

    @Column(nullable = false)
    private boolean isAccepted = false;

    @Column(nullable = false)
    LocalDateTime requestedAt;

    LocalDateTime acceptedAt;

    @PrePersist
    protected void onCreate() {
        requestedAt = LocalDateTime.now();
    }
}
