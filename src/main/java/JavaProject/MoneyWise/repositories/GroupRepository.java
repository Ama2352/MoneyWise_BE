package JavaProject.MoneyWise.repositories;

import JavaProject.MoneyWise.models.entities.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface GroupRepository extends JpaRepository<Group, UUID> {
    @Query("SELECT g FROM Group g LEFT JOIN FETCH g.members WHERE g.groupId = :groupId")
    Optional<Group> findByIdWithMembers(UUID groupId);
}
