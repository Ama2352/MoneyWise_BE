package JavaProject.MoneyManagement_BE_SE330.helper;

import JavaProject.MoneyManagement_BE_SE330.models.entities.User;
import JavaProject.MoneyManagement_BE_SE330.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;

@RequiredArgsConstructor
public class HelperFunctions {
    public static User getCurrentUser(UserRepository userRepository) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public static User findUserByStringId(String userId, UserRepository userRepository) {
        Long userLongId = Long.parseLong(userId);
        return userRepository.findById(userLongId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}

