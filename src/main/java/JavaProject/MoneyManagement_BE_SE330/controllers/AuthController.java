package JavaProject.MoneyManagement_BE_SE330.controllers;

import JavaProject.MoneyManagement_BE_SE330.models.dtos.auth.LoginDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.auth.RegisterDTO;
import JavaProject.MoneyManagement_BE_SE330.services.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/Accounts")
@Tag(name = "Authentication")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/SignIn")
    public ResponseEntity<String> login(@RequestBody LoginDTO request) {
        String token = authService.authenticate(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(token);
    }

    @PostMapping("/SignUp")
    public ResponseEntity<Boolean> register(@RequestBody RegisterDTO dto) {
        boolean success = authService.register(dto);
        return ResponseEntity.ok(success);
    }
}
