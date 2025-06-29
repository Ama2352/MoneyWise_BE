package JavaProject.MoneyWise.models.dtos.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class LoginDTO {

    @Schema(example = "demo3@example.com")
    private String email;

    @Schema(example = "Demo3@123")
    private String password;
}
