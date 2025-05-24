package JavaProject.MoneyManagement_BE_SE330.models.dtos.group;

import JavaProject.MoneyManagement_BE_SE330.models.enums.GroupRole;
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
