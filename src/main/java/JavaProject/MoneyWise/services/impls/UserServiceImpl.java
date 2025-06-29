package JavaProject.MoneyWise.services.impls;

import JavaProject.MoneyWise.helper.ApplicationMapper;
import JavaProject.MoneyWise.helper.HelperFunctions;
import JavaProject.MoneyWise.helper.ResourceNotFoundException;
import JavaProject.MoneyWise.helper.ValidationException;
import JavaProject.MoneyWise.models.dtos.profile.UpdateProfileDTO;
import JavaProject.MoneyWise.models.dtos.profile.UserProfileDTO;
import JavaProject.MoneyWise.models.entities.User;
import JavaProject.MoneyWise.repositories.UserRepository;
import JavaProject.MoneyWise.services.ImageService;
import JavaProject.MoneyWise.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final ImageService imageService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationMapper applicationMapper;

    // Regex: At least 1 uppercase, 1 special character, minimum 6 characters
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Z])(?=.*[@#$%^&+=!]).{6,}$");

    @Transactional
    @Override
    public String updateUserAvatar(UUID userId, MultipartFile file) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Delete old avatar if exists
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            imageService.deleteImage(user.getAvatarUrl());
        }

        // Upload new avatar
        String avatarUrl = imageService.uploadImage(file, userId);
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        return avatarUrl;
    }

    public String getUserAvatar(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        return user.getAvatarUrl() != null ? user.getAvatarUrl() : getDefaultAvatarUrl();
    }

    private String getDefaultAvatarUrl() {
        return "https://www.pngplay.com/wp-content/uploads/12/User-Avatar-Profile-Transparent-Clip-Art-PNG.png";
    }

    @Override
    public User getCurrentUser() {
        return HelperFunctions.getCurrentUser(userRepository);
    }

    @Override
    public UserProfileDTO getUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("\"User not found with ID: \" + userId)"));
        return applicationMapper.toUserProfileDTO(user);
    }

    @Override
    public UserProfileDTO updateCurrentUserProfile(UpdateProfileDTO model) {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        Map<String, List<String>> errors = new HashMap<>();

        // Check if password update is requested (newPassword and confirmNewPassword are provided)
        if (model.getNewPassword() != null && !model.getNewPassword().isEmpty()) {
            // Validate current password
            if (!passwordEncoder.matches(model.getCurrentPassword(), currentUser.getPassword())) {
                errors.put("CurrentPassword", List.of("The current password is incorrect."));
            }

            // Validate new password pattern
            if (!PASSWORD_PATTERN.matcher(model.getNewPassword()).matches()) {
                errors.put("NewPassword", List.of("New password must contain at least 6 characters, including 1 uppercase, 1 special character."));
            }

            // Validate password confirmation
            if (!model.getNewPassword().equals(model.getConfirmNewPassword())) {
                errors.put("ConfirmNewPassword", List.of("The password and confirmation password do not match."));
            }
        } else if (model.getCurrentPassword() != null && !model.getCurrentPassword().isEmpty()) {
            // Require current password for non-password field updates
            if (!passwordEncoder.matches(model.getCurrentPassword(), currentUser.getPassword())) {
                errors.put("CurrentPassword", List.of("The current password is incorrect."));
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        // Update non-password fields only if validations pass
        if (model.getFirstName() != null) {
            currentUser.setFirstName(model.getFirstName());
        }
        if (model.getLastName() != null) {
            currentUser.setLastName(model.getLastName());
        }

        // Update password only if newPassword is provided
        if (model.getNewPassword() != null && !model.getNewPassword().isEmpty()) {
            String encoded = passwordEncoder.encode(model.getNewPassword());
            currentUser.setPassword(encoded);
        }

        userRepository.save(currentUser);

        return applicationMapper.toUserProfileDTO(currentUser);
    }

    @Transactional
    @Override
    public void deleteUserAvatar(UUID userId) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            imageService.deleteImage(user.getAvatarUrl());
            user.setAvatarUrl(null);
            userRepository.save(user);
        }
    }
}
