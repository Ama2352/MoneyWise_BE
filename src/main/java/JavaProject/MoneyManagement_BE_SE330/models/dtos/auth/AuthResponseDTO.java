package JavaProject.MoneyManagement_BE_SE330.models.dtos.auth;

import lombok.Data;

@Data
public class AuthResponseDTO {
    private String accessToken;
    private boolean success;
    private String[] errors;

    public AuthResponseDTO() {
        this.success = true;
        this.errors = new String[0];
    }

    public AuthResponseDTO(String accessToken, boolean success, String[] errors) {
        this.accessToken = accessToken;
        this.success = success;
        this.errors = errors;
    }
}