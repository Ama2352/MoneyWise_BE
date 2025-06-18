package JavaProject.MoneyWise.models.dtos.group;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminLeaveResult {
    private boolean success;
    private String action; // leave or delete
    private UUID groupId;
    private String newAdminId; // If action is "leave"
}
