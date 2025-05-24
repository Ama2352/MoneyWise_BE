package JavaProject.MoneyManagement_BE_SE330.models.entities;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFriendId implements Serializable {

    private Long userId;
    private Long friendId;

    // Override equals and hashCode for composite key equality

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserFriendId that)) return false;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(friendId, that.friendId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, friendId);
    }
}