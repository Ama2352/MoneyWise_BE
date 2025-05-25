package JavaProject.MoneyManagement_BE_SE330.controllers;

import JavaProject.MoneyManagement_BE_SE330.helper.HelperFunctions;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.auth.LoginDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.auth.RegisterDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.profile.UpdateProfileDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.profile.UserProfileDTO;
import JavaProject.MoneyManagement_BE_SE330.models.entities.User;
import JavaProject.MoneyManagement_BE_SE330.repositories.UserRepository;
import JavaProject.MoneyManagement_BE_SE330.services.AuthService;
import JavaProject.MoneyManagement_BE_SE330.services.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<UserProfileDTO> getOtherUserProfile(@PathVariable("userId") UUID userId ) {
        UserProfileDTO dto = userService.getUserProfile(userId);
        return ResponseEntity.ok(dto);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/profile")
    public ResponseEntity<UserProfileDTO> updateUserProfile(@RequestBody @Valid UpdateProfileDTO dto) {
        UserProfileDTO updatedProfile = userService.updateCurrentUserProfile(dto);
        return ResponseEntity.ok(updatedProfile);
    }
}
