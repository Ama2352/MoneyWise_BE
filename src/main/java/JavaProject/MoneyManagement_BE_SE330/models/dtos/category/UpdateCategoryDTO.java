package JavaProject.MoneyManagement_BE_SE330.models.dtos.category;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UpdateCategoryDTO {

    @NotNull(message = "CategoryId must not be null")
    private UUID categoryId;

    @NotNull(message = "CategoryName must not be null")
    private String name;
}
