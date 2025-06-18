package JavaProject.MoneyWise.repositories;

import JavaProject.MoneyWise.models.entities.Category;
import JavaProject.MoneyWise.models.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findAllByUser(User user);

    Optional<Category> findByCategoryIdAndUser(UUID categoryId, User user);

    boolean existsByUser(User user);

    boolean existsByCategoryIdAndUser(UUID categoryId, User user);

    void deleteByCategoryIdAndUser(UUID categoryId, User user);
}
