package JavaProject.MoneyWise.services;

import JavaProject.MoneyWise.models.dtos.auth.RegisterDTO;

public interface AuthService {
    String authenticate(String email, String password);
    boolean register(RegisterDTO dto);;
}