package JavaProject.MoneyWise.services;

import JavaProject.MoneyWise.models.dtos.profile.UpdateProfileDTO;
import JavaProject.MoneyWise.models.dtos.profile.UserProfileDTO;
import JavaProject.MoneyWise.models.entities.User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface UserService {
    String updateUserAvatar(UUID userId, MultipartFile file) throws Exception;
    User getCurrentUser();
    UserProfileDTO getUserProfile(UUID userId);
    UserProfileDTO updateCurrentUserProfile(UpdateProfileDTO model);
    void deleteUserAvatar(UUID userId) throws Exception;
}
