package JavaProject.MoneyWise.models.dtos.auth;

import lombok.Data;

@Data
public class RefreshTokenResponseDTO {
    private boolean success;
    private String message;
    private String token;

    private RefreshTokenResponseDTO(boolean success, String message, String token) {
        this.success = success;
        this.message = message;
        this.token = token;
    }

    public static RefreshTokenResponseDTO success(String token) {
        return new RefreshTokenResponseDTO(true, "Token refreshed successfully", token);
    }

    public static RefreshTokenResponseDTO error(String errorMessage) {
        return new RefreshTokenResponseDTO(false, errorMessage, null);
    }
}