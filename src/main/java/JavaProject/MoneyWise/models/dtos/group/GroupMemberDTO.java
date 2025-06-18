package JavaProject.MoneyWise.models.dtos.group;

import JavaProject.MoneyWise.models.enums.GroupRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberDTO {
    private String userId;
    private String displayName;
    private String avatarUrl;
    private GroupRole role;
    private LocalDateTime joinedAt;
}
