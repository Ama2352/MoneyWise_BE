package JavaProject.MoneyManagement_BE_SE330.models.dtos.group;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreateGroupDTO {

    @NotNull(message = "Group's name must not be null")
    private String name;

    private String description;
    private List<String> initialMemberIds;
}
