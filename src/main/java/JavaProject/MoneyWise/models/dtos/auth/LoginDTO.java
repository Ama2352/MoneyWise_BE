package JavaProject.MoneyWise.models.dtos.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class LoginDTO {

    @Schema(example = "demo@example.com")
    private String email;

    @Schema(example = "Demo@123")
    private String password;
}
