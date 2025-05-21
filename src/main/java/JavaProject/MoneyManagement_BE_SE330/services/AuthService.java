package JavaProject.MoneyManagement_BE_SE330.services;

import JavaProject.MoneyManagement_BE_SE330.models.dtos.auth.RegisterDTO;

public interface AuthService {
    String authenticate(String email, String password);
    boolean register(RegisterDTO dto);
}
