package JavaProject.MoneyWise.models.dtos.profile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    private String id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String displayName;
    private String avatarUrl;
}
