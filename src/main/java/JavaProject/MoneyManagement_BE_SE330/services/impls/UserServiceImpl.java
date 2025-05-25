package JavaProject.MoneyManagement_BE_SE330.services.impls;

import JavaProject.MoneyManagement_BE_SE330.helper.ApplicationMapper;
import JavaProject.MoneyManagement_BE_SE330.helper.HelperFunctions;
import JavaProject.MoneyManagement_BE_SE330.helper.ResourceNotFoundException;
import JavaProject.MoneyManagement_BE_SE330.helper.ValidationException;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.profile.UpdateProfileDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.profile.UserProfileDTO;
import JavaProject.MoneyManagement_BE_SE330.models.entities.User;
import JavaProject.MoneyManagement_BE_SE330.repositories.UserRepository;
import JavaProject.MoneyManagement_BE_SE330.services.ImageService;
import JavaProject.MoneyManagement_BE_SE330.services.UserService;
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

        if(!passwordEncoder.matches(model.getCurrentPassword(), currentUser.getPassword())) {
            errors.put("CurrentPassword", List.of("The current password is incorrect."));
        }

        if (!PASSWORD_PATTERN.matcher(model.getNewPassword()).matches()) {
            errors.put("NewPassword", List.of("New password must contain at least 6 characters, including 1 uppercase, 1 special character."));
        }

        if (!model.getNewPassword().equals(model.getConfirmNewPassword())) {
            errors.put("ConfirmNewPassword", List.of("The password and confirmation password do not match."));
        }

        if(!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        currentUser.setFirstName(model.getFirstName());
        currentUser.setLastName(model.getLastName());
        String encoded = passwordEncoder.encode(model.getNewPassword());
        currentUser.setPassword(encoded);

        userRepository.save(currentUser);

        return applicationMapper.toUserProfileDTO(currentUser);
    }
}
