package JavaProject.MoneyManagement_BE_SE330.models.dtos.auth;

import lombok.Data;

@Data
public class AuthResponseDTO {
    private String accessToken;
    private String refreshToken;
}