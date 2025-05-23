package JavaProject.MoneyManagement_BE_SE330.controllers;

import JavaProject.MoneyManagement_BE_SE330.models.dtos.auth.AuthResponseDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.auth.LoginDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.auth.RefreshTokenRequestDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.auth.RegisterDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.geminiOCR.OcrTextRequestDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.geminiOCR.TransactionInfoDTO;
import JavaProject.MoneyManagement_BE_SE330.models.entities.RefreshToken;
import JavaProject.MoneyManagement_BE_SE330.services.AuthService;
import JavaProject.MoneyManagement_BE_SE330.services.GeminiService;
import JavaProject.MoneyManagement_BE_SE330.services.JwtService;
import JavaProject.MoneyManagement_BE_SE330.services.RefreshTokenService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
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
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final GeminiService geminiService;

    @PostMapping("/SignIn")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody LoginDTO request) {
        return ResponseEntity.ok(authService.authenticate(request.getEmail(), request.getPassword()));
    }

    @PostMapping("/SignUp")
    public ResponseEntity<Boolean> register(@RequestBody RegisterDTO dto) {
        boolean success = authService.register(dto);
        return ResponseEntity.ok(success);
    }

    @PostMapping("/RefreshToken")
    public ResponseEntity<AuthResponseDTO> refreshToken(@RequestBody RefreshTokenRequestDTO request) {
        String refreshToken = request.getRefreshToken();
        RefreshToken token = refreshTokenService.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
        refreshTokenService.verifyExpiration(token);

        String username = token.getUser().getUsername(); // Get username from RefreshToken entity
        var userDetails = userDetailsService.loadUserByUsername(username);

        // Generate new access token
        String newAccessToken = jwtService.generateToken(userDetails);

        // Rotate refresh token
        refreshTokenService.deleteByUser(token.getUser());
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(username);

        AuthResponseDTO response = new AuthResponseDTO();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(newRefreshToken.getToken());
        return ResponseEntity.ok(response);
    }

}
