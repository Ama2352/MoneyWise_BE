package JavaProject.MoneyWise.models.dtos.category;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreateCategoryDTO {

    @NotNull(message = "CategoryName must not be null")
    private String name;
}
