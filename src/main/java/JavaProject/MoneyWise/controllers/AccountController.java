package JavaProject.MoneyWise.controllers;

import JavaProject.MoneyWise.models.dtos.auth.LoginDTO;
import JavaProject.MoneyWise.models.dtos.auth.RefreshTokenRequestDTO;
import JavaProject.MoneyWise.models.dtos.auth.RefreshTokenResponseDTO;
import JavaProject.MoneyWise.models.dtos.auth.RegisterDTO;
import JavaProject.MoneyWise.models.dtos.profile.UpdateProfileDTO;
import JavaProject.MoneyWise.models.dtos.profile.UserProfileDTO;
import JavaProject.MoneyWise.models.entities.User;
import JavaProject.MoneyWise.services.AuthService;
import JavaProject.MoneyWise.services.JwtService;
import JavaProject.MoneyWise.services.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/Accounts")
@Tag(name = "Accounts")
public class AccountController {
    private final AuthService authService;
    private final UserService userService;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @PostMapping("/SignIn")
    public ResponseEntity<String> login(@RequestBody LoginDTO request) {
        String accessToken = authService.authenticate(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(accessToken);
    }

    @PostMapping("/SignUp")
    public ResponseEntity<Boolean> register(@RequestBody RegisterDTO dto) {
        boolean success = authService.register(dto);
        return ResponseEntity.ok(success);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(
            value = "/avatar",
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file) throws Exception {
        User currentUser = userService.getCurrentUser();
        String avatarUrl = userService.updateUserAvatar(currentUser.getId(), file);
        return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/profile")
    public ResponseEntity<UserProfileDTO> getUserProfile() {
        User currentUser = userService.getCurrentUser();
        UserProfileDTO dto = userService.getUserProfile(currentUser.getId());
        return ResponseEntity.ok(dto);
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserProfileDTO> getOtherUserProfile(@PathVariable("userId") UUID userId) {
        UserProfileDTO dto = userService.getUserProfile(userId);
        return ResponseEntity.ok(dto);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/profile")
    public ResponseEntity<UserProfileDTO> updateUserProfile(@RequestBody @Valid UpdateProfileDTO dto) {
        UserProfileDTO updatedProfile = userService.updateCurrentUserProfile(dto);
        return ResponseEntity.ok(updatedProfile);
    }

    @PostMapping("/RefreshToken")
    public ResponseEntity<RefreshTokenResponseDTO> refreshToken(@RequestBody RefreshTokenRequestDTO request) {
        try {
            String expiredAccessToken = request.getExpiredToken();
            if (expiredAccessToken == null || expiredAccessToken.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        RefreshTokenResponseDTO.error("Access token is required")
                );
            }

            // Validate expired access token
            String username = jwtService.validateExpiredToken(expiredAccessToken);
            if (username == null) {
                return ResponseEntity.badRequest().body(
                        RefreshTokenResponseDTO.error("Invalid access token")
                );
            }

            // Load user details
            var userDetails = userDetailsService.loadUserByUsername(username);

            // Generate new access token
            String newAccessToken = jwtService.generateToken(userDetails);

            return ResponseEntity.ok(RefreshTokenResponseDTO.success(newAccessToken));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    RefreshTokenResponseDTO.error(e.getMessage())
            );
        }
    }
}
