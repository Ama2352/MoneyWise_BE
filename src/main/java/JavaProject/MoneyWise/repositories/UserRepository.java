package JavaProject.MoneyWise.repositories;

import JavaProject.MoneyWise.models.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username); // Added for userId lookup
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
