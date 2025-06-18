package JavaProject.MoneyWise.models.dtos.profile;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileDTO {
    private String firstName;
    private String lastName;

    @NotNull(message = "Current password must not be null")
    private String currentPassword;

    private String newPassword;
    private String confirmNewPassword;
}
