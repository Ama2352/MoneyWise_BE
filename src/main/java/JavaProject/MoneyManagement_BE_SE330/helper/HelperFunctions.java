package JavaProject.MoneyManagement_BE_SE330.helper;

import JavaProject.MoneyManagement_BE_SE330.models.entities.User;
import JavaProject.MoneyManagement_BE_SE330.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public class HelperFunctions {
    public static User getCurrentUser(UserRepository userRepository) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public static User findUserByStringId(String userId, UserRepository userRepository) {
        log.info("Received userId: {}", userId);
        UUID uuid = UUID.fromString(userId);  // 🔄 Convert String to UUID
        return userRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}

