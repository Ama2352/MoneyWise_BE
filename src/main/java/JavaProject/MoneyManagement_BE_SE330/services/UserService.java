package JavaProject.MoneyManagement_BE_SE330.services;

import JavaProject.MoneyManagement_BE_SE330.models.dtos.profile.UpdateProfileDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.profile.UserProfileDTO;
import JavaProject.MoneyManagement_BE_SE330.models.entities.User;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    String updateUserAvatar(Long userId, MultipartFile file) throws Exception;
    User getCurrentUser();
    UserProfileDTO getUserProfile(Long userId);
    UserProfileDTO updateCurrentUserProfile(UpdateProfileDTO model);
}
