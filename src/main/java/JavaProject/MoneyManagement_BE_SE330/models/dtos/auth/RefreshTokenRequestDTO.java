package JavaProject.MoneyManagement_BE_SE330.models.dtos.auth;

import lombok.Data;

@Data
public class RefreshTokenRequestDTO {
    private String expiredToken;
}