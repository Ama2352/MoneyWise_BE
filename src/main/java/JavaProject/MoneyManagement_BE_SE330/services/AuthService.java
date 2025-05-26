package JavaProject.MoneyManagement_BE_SE330.services;

import JavaProject.MoneyManagement_BE_SE330.models.dtos.auth.AuthResponseDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.auth.RegisterDTO;

public interface AuthService {
    AuthResponseDTO authenticate(String email, String password);
    boolean register(RegisterDTO dto);
}