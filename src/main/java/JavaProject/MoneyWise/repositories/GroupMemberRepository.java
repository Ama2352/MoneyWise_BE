package JavaProject.MoneyWise.repositories;

import JavaProject.MoneyWise.models.entities.GroupMember;
import JavaProject.MoneyWise.models.enums.GroupRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {
    int countByGroupGroupId(UUID groupId);
    List<GroupMember> findAllByUserId(UUID userId);

    boolean existsByGroup_GroupIdAndUser_Id(UUID groupId, UUID userId);

    @Query("SELECT gm FROM GroupMember gm JOIN FETCH gm.user WHERE gm.group.groupId = :groupId")
    List<GroupMember> findByGroup_GroupIdWithUser(UUID groupId);

    Optional<GroupMember> findByGroup_GroupIdAndUser_Id(UUID groupId, UUID userId);

    void deleteByGroupGroupId(UUID groupId);

    @Query("SELECT gm FROM GroupMember gm WHERE gm.group.groupId = :groupId AND gm.user.id != :excludeUserId ORDER BY CASE gm.role WHEN 'COLLABORATOR' THEN 1 WHEN 'MEMBER' THEN 2 ELSE 3 END, gm.joinedAt ASC")
    Optional<GroupMember> findNextAdmin(@Param("groupId") UUID groupId, @Param("excludeUserId") UUID excludeUserId);

    boolean existsByGroupGroupIdAndUserIdAndRole(UUID groupId, UUID userId, GroupRole role);

    long countByGroupGroupIdAndRole(UUID groupId, GroupRole groupRole);
}