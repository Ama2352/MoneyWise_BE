package JavaProject.MoneyWise.models.dtos.auth;

import lombok.Data;

@Data
public class RefreshTokenRequestDTO {
    private String expiredToken;
}