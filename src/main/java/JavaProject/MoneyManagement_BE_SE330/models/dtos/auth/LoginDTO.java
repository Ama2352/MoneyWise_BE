package JavaProject.MoneyManagement_BE_SE330.models.dtos.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class LoginDTO {

    @Schema(example = "mon@example.com")
    private String email;

    @Schema(example = "Mon@123")
    private String password;
}
