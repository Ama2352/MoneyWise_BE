package JavaProject.MoneyWise.services;

import JavaProject.MoneyWise.models.dtos.group.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface GroupService {
    GroupDTO createGroup(String creatorId, CreateGroupDTO dto);
    List<GroupDTO> getUserGroups(String userId);
    List<GroupMemberDTO> getGroupMembers(String userId, UUID groupId);

    @Transactional
    boolean addUserToGroup(String adminUserId, UUID groupId, String newUserId);

    @Transactional
    boolean removeUserFromGroup(String adminUserId, UUID groupId, String userToRemoveId);

    @Transactional
    boolean updateGroup(String adminUserId, UUID groupId, UpdateGroupDTO dto);

    @Transactional
    boolean assignCollaboratorRole(String adminUserId, UUID groupId, String userId);

    @Transactional
    boolean deleteGroup(String adminUserId, UUID groupId);

    @Transactional
    AdminLeaveResult adminLeaveGroup(String adminUserId, UUID groupId, boolean deleteGroup);

    boolean leaveGroup(String userId, UUID groupId);
}
