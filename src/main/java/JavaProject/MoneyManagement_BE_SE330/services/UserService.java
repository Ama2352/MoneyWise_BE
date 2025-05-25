package JavaProject.MoneyManagement_BE_SE330.services;

import JavaProject.MoneyManagement_BE_SE330.models.dtos.profile.UpdateProfileDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.profile.UserProfileDTO;
import JavaProject.MoneyManagement_BE_SE330.models.entities.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface UserService {
    String updateUserAvatar(UUID userId, MultipartFile file) throws Exception;
    User getCurrentUser();
    UserProfileDTO getUserProfile(UUID userId);
    UserProfileDTO updateCurrentUserProfile(UpdateProfileDTO model);
}
