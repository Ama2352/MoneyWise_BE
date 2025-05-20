package JavaProject.MoneyManagement_BE_SE330.repositories;

import JavaProject.MoneyManagement_BE_SE330.models.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username); // Added for userId lookup
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
