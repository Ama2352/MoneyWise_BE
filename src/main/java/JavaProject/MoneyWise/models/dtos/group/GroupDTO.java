package JavaProject.MoneyWise.models.dtos.group;

import JavaProject.MoneyWise.models.enums.GroupRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupDTO {
    private UUID groupId;
    private String name;
    private String description;
    private String imageUrl;
    private LocalDateTime createdAt;
    private String creatorId;
    private String creatorName;
    private int memberCount;
    private GroupRole role; // of current user
}
