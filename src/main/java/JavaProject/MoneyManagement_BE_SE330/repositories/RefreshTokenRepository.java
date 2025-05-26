package JavaProject.MoneyManagement_BE_SE330.repositories;

import JavaProject.MoneyManagement_BE_SE330.models.entities.RefreshToken;
import JavaProject.MoneyManagement_BE_SE330.models.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUser(User user);
}