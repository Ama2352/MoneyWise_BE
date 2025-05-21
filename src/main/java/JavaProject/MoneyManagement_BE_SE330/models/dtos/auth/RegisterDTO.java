package JavaProject.MoneyManagement_BE_SE330.models.dtos.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class RegisterDTO {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String confirmedPassword;
}
