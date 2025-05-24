package JavaProject.MoneyManagement_BE_SE330.models.dtos.group;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGroupDTO {
    private String name;
    private String description;
}
