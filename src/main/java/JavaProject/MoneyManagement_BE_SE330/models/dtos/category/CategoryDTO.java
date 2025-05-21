package JavaProject.MoneyManagement_BE_SE330.models.dtos.category;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CategoryDTO {
    private UUID categoryId;
    private String name;
    private LocalDateTime createdAt;
}
