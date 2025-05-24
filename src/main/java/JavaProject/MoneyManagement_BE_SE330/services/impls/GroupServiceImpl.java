package JavaProject.MoneyManagement_BE_SE330.services.impls;

import JavaProject.MoneyManagement_BE_SE330.helper.ApplicationMapper;
import JavaProject.MoneyManagement_BE_SE330.helper.HelperFunctions;
import JavaProject.MoneyManagement_BE_SE330.helper.ResourceNotFoundException;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.group.*;
import JavaProject.MoneyManagement_BE_SE330.models.entities.Group;
import JavaProject.MoneyManagement_BE_SE330.models.entities.GroupMember;
import JavaProject.MoneyManagement_BE_SE330.models.entities.User;
import JavaProject.MoneyManagement_BE_SE330.models.enums.GroupRole;
import JavaProject.MoneyManagement_BE_SE330.repositories.GroupMemberRepository;
import JavaProject.MoneyManagement_BE_SE330.repositories.GroupRepository;
import JavaProject.MoneyManagement_BE_SE330.repositories.UserRepository;
import JavaProject.MoneyManagement_BE_SE330.services.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private final ApplicationMapper applicationMapper;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    @Override
    public GroupDTO createGroup(String creatorId, CreateGroupDTO dto) {

        // Create and map the group
        Group newGroup = applicationMapper.toGroupEntity(dto);
        groupRepository.save(newGroup);

        // Add creator as first admin member
        GroupMember creatorMember = new GroupMember(); // "id" and "joinedAt" is automatically generated
        creatorMember.setGroup(newGroup);
        Long creatorLongId = Long.parseLong(creatorId);
        User creator = userRepository.findById(creatorLongId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        creatorMember.setUser(creator);
        creatorMember.setRole(GroupRole.ADMIN);
        groupMemberRepository.save(creatorMember);

        // Add initial members if provided
        if(dto.getInitialMemberIds() != null && !dto.getInitialMemberIds().isEmpty()) {
            for(String memberId : dto.getInitialMemberIds()) {
                if(!memberId.equals(creatorId)) {
                    GroupMember newMember = new GroupMember();
                    newMember.setGroup(newGroup);
                    newMember.setUser(HelperFunctions.findUserByStringId(memberId, userRepository));
                    newMember.setRole(GroupRole.MEMBER);
                    groupMemberRepository.save(newMember);
                }
            }
        }

        // Mapping response
        int memberCount = groupMemberRepository.countByGroupGroupId(newGroup.getGroupId());
        GroupDTO groupDTO = applicationMapper.toGroupDTO(newGroup, memberCount);

        // Notify all members (including creator)
        List<String> memberIds = new ArrayList<>();
        memberIds.add(creatorId);
        if(dto.getInitialMemberIds() != null) {
            memberIds.addAll(dto.getInitialMemberIds());
        }
        for(String memberId : memberIds) {
            messagingTemplate.convertAndSendToUser(
                    memberId,
                    "/topic/group-created",
                    groupDTO
            );
        }

        return groupDTO;
    }

    @Transactional
    @Override
    public List<GroupDTO> getUserGroups(String userId) {
        Long userLongId = Long.parseLong(userId);

        // Get all GroupMember entities for this user, fetch groups eagerly if needed
        List<GroupMember> groupMembers = groupMemberRepository.findAllByUserId(userLongId);

        List<GroupDTO> groups = new ArrayList<>();

        for (GroupMember member : groupMembers) {
            Group group = member.getGroup();

            // Map Group entity to GroupDTO using your mapper
            GroupDTO groupDTO = applicationMapper.toGroupDTO(group, 0); // memberCount is 0 for now

            // Set creator full name (assuming Group has getCreator() method returning User)
            User creator = group.getCreator(); // or however your Group stores creator
            if (creator != null) {
                String creatorName = creator.getFirstName() + " " + creator.getLastName();
                groupDTO.setCreatorName(creatorName);
            } else {
                groupDTO.setCreatorName("Unknown");
            }

            // Get member count for this group
            int memberCount = groupMemberRepository.countByGroupGroupId(group.getGroupId());
            groupDTO.setMemberCount(memberCount);

            // Set the role of the current user in this group
            groupDTO.setRole(member.getRole());

            groups.add(groupDTO);
        }

        return groups;
    }

    @Transactional
    @Override
    public List<GroupMemberDTO> getGroupMembers(String userId, UUID groupId) {
        Long userLongId = Long.parseLong(userId);

        // Check if user is a member of the group
        boolean isMember = groupMemberRepository.existsByGroup_GroupIdAndUser_Id(groupId, userLongId);
        if (!isMember) {
            throw new AccessDeniedException("User is not a member of this group");
        }

        // Get all members of the group (including user details)
        List<GroupMember> members = groupMemberRepository.findByGroup_GroupIdWithUser(groupId);

        // Map members to DTOs
        List<GroupMemberDTO> memberDTOs = new ArrayList<>();
        for (GroupMember member : members) {
            GroupMemberDTO dto = applicationMapper.toGroupMemberDTO(member);
            memberDTOs.add(dto);
        }

        return memberDTOs;
    }

    @Transactional
    @Override
    public boolean addUserToGroup(String adminUserId, UUID groupId, String newUserId) {
        // Verify the current user is an admin or collaborator of the group
        Long adminLongId = Long.parseLong(adminUserId);
        GroupMember adminMember = groupMemberRepository.findByGroup_GroupIdAndUser_Id(groupId, adminLongId)
                .orElseThrow(() -> new AccessDeniedException("Only group admins or collaborators can add members"));

        if (adminMember.getRole() != GroupRole.ADMIN && adminMember.getRole() != GroupRole.COLLABORATOR) {
            throw new AccessDeniedException("Only group admins or collaborators can add members");
        }

        // Check if user is already a member
        Long newUserLongId = Long.parseLong(newUserId);
        boolean isAlreadyMember = groupMemberRepository.existsByGroup_GroupIdAndUser_Id(groupId, newUserLongId);
        if (isAlreadyMember) {
            return false; // Already a member
        }

        // Verify group exists
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        // Verify new user exists
        User newUser = userRepository.findById(newUserLongId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Add the new member
        GroupMember newMember = new GroupMember();
        newMember.setGroup(group);
        newMember.setUser(newUser);
        newMember.setRole(GroupRole.MEMBER);
        groupMemberRepository.save(newMember);

        // Notify the new member via WebSocket
        GroupDTO groupDTO = applicationMapper.toGroupDTO(group, groupMemberRepository.countByGroupGroupId(groupId));
        messagingTemplate.convertAndSendToUser(
                newUserId,
                "/topic/group-joined",
                groupDTO
        );

        return true;
    }

    @Transactional
    @Override
    public boolean removeUserFromGroup(String adminUserId, UUID groupId, String userToRemoveId) {
        // Special case: user removing themselves (leaving group)
        boolean isSelfRemoval = adminUserId.equals(userToRemoveId);

        // Find the membership to remove
        Long userToRemoveLongId = Long.parseLong(userToRemoveId);
        Optional<GroupMember> memberToRemoveOpt = groupMemberRepository.findByGroup_GroupIdAndUser_Id(groupId, userToRemoveLongId);
        if (memberToRemoveOpt.isEmpty()) {
            return false; // Not a member
        }
        GroupMember memberToRemove = memberToRemoveOpt.get();

        // If not self-removal, check if admin
        if (!isSelfRemoval) {
            // Verify the current user is an admin or collaborator of the group
            Long adminLongId = Long.parseLong(adminUserId);
            GroupMember adminMember = groupMemberRepository.findByGroup_GroupIdAndUser_Id(groupId, adminLongId)
                    .orElseThrow(() -> new AccessDeniedException("Only group admins or collaborators can remove members"));

            if (adminMember.getRole() != GroupRole.ADMIN && adminMember.getRole() != GroupRole.COLLABORATOR) {
                throw new AccessDeniedException("Only group admins or collaborators can remove members");
            }

            // Collaborators can't remove admins
            if (adminMember.getRole() == GroupRole.COLLABORATOR && memberToRemove.getRole() == GroupRole.ADMIN) {
                throw new AccessDeniedException("Collaborators cannot remove admins");
            }
        }

        // Check if removing the last admin (only applies to admin self-removal)
        if (memberToRemove.getRole() == GroupRole.ADMIN && isSelfRemoval) {
            long adminCount = groupMemberRepository.countByGroupGroupIdAndRole(groupId, GroupRole.ADMIN);
            if (adminCount <= 1) {
                throw new IllegalStateException("As the last admin, you must use the admin leave process");
            }
        }

        // Remove the member
        groupMemberRepository.delete(memberToRemove);

        // Notify the removed user via WebSocket
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        GroupDTO groupDTO = applicationMapper.toGroupDTO(group, groupMemberRepository.countByGroupGroupId(groupId));
        messagingTemplate.convertAndSendToUser(
                userToRemoveId,
                "/topic/group-removed",
                groupDTO
        );

        return true;
    }

    @Transactional
    @Override
    public boolean updateGroup(String adminUserId, UUID groupId, UpdateGroupDTO dto) {
        // Verify the current user is an admin of the group
        Long adminLongId = Long.parseLong(adminUserId);
        boolean isAdmin = groupMemberRepository.existsByGroupGroupIdAndUserIdAndRole(groupId, adminLongId, GroupRole.ADMIN);
        if (!isAdmin) {
            throw new AccessDeniedException("Only group admins can update group");
        }

        // Find the group
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group with ID " + groupId + " not found"));

        // Update properties
        if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
            group.setName(dto.getName());
        }

        if (dto.getDescription() != null) {
            group.setDescription(dto.getDescription());
        }

        // Save changes
        groupRepository.save(group);

        // Notify all group members via WebSocket
        List<GroupMember> members = groupMemberRepository.findByGroup_GroupIdWithUser(groupId);
        GroupDTO groupDTO = applicationMapper.toGroupDTO(group, groupMemberRepository.countByGroupGroupId(groupId));
        for (GroupMember member : members) {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(member.getUser().getId()),
                    "/topic/group-updated",
                    groupDTO
            );
        }

        return true;
    }

    @Transactional
    @Override
    public boolean assignCollaboratorRole(String adminUserId, UUID groupId, String userId) {
        // Verify the current user is an admin of the group
        Long adminLongId = Long.parseLong(adminUserId);
        GroupMember adminMember = groupMemberRepository.findByGroup_GroupIdAndUser_Id(groupId, adminLongId)
                .orElseThrow(() -> new AccessDeniedException("Only group admins can assign roles"));

        if (adminMember.getRole() != GroupRole.ADMIN) {
            throw new AccessDeniedException("Only group admins can assign roles");
        }

        // Find the member to promote
        Long userLongId = Long.parseLong(userId);
        GroupMember member = groupMemberRepository.findByGroup_GroupIdAndUser_Id(groupId, userLongId)
                .orElseThrow(() -> new ResourceNotFoundException("User " + userId + " is not a member of this group"));

        // Assign collaborator role
        member.setRole(GroupRole.COLLABORATOR);
        groupMemberRepository.save(member);

        // Notify the promoted user via WebSocket
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        GroupDTO groupDTO = applicationMapper.toGroupDTO(group, groupMemberRepository.countByGroupGroupId(groupId));
        messagingTemplate.convertAndSendToUser(
                userId,
                "/topic/role-updated",
                groupDTO
        );

        return true;
    }

    @Transactional
    @Override
    public boolean deleteGroup(String adminUserId, UUID groupId) {
        // Verify the current user is an admin of the group
        Long adminLongId = Long.parseLong(adminUserId);
        GroupMember adminMember = groupMemberRepository.findByGroup_GroupIdAndUser_Id(groupId, adminLongId)
                .orElseThrow(() -> new AccessDeniedException("Only group admins can delete groups"));

        if (adminMember.getRole() != GroupRole.ADMIN) {
            throw new AccessDeniedException("Only group admins can delete groups");
        }

        // Find the group with members
        Group group = groupRepository.findByIdWithMembers(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group with ID " + groupId + " not found"));

        // Notify all group members via WebSocket before deletion
        GroupDTO groupDTO = applicationMapper.toGroupDTO(group, groupMemberRepository.countByGroupGroupId(groupId));
        for (GroupMember member : group.getMembers()) {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(member.getUser().getId()),
                    "/topic/group-deleted",
                    groupDTO
            );
        }

        // Remove all memberships
        groupMemberRepository.deleteByGroupGroupId(groupId);

        // Remove the group
        groupRepository.delete(group);

        return true;
    }

    @Transactional
    @Override
    public AdminLeaveResult adminLeaveGroup(String adminUserId, UUID groupId, boolean deleteGroup) {
        // Verify user is an admin
        Long adminLongId = Long.parseLong(adminUserId);
        GroupMember adminMember = groupMemberRepository.findByGroup_GroupIdAndUser_Id(groupId, adminLongId)
                .orElseThrow(() -> new ResourceNotFoundException("User is not a member of this group"));

        if (adminMember.getRole() != GroupRole.ADMIN) {
            throw new AccessDeniedException("Only admins can use this function");
        }

        // If delete option is selected, delete the group entirely
        if (deleteGroup) {
            boolean deleted = deleteGroup(adminUserId, groupId);
            AdminLeaveResult result = new AdminLeaveResult();
            result.setSuccess(deleted);
            result.setAction("delete");
            result.setGroupId(groupId);
            return result;
        }

        // Find next admin based on role precedence and join time
        GroupMember nextAdmin = groupMemberRepository.findNextAdmin(groupId, adminLongId)
                .orElse(null);

        if (nextAdmin == null) {
            // If no other members, delete the group
            boolean deleted = deleteGroup(adminUserId, groupId);
            AdminLeaveResult result = new AdminLeaveResult();
            result.setSuccess(deleted);
            result.setAction("delete");
            result.setGroupId(groupId);
            return result;
        }

        // Promote the next admin
        nextAdmin.setRole(GroupRole.ADMIN);
        groupMemberRepository.save(nextAdmin);

        // Remove the current admin from the group
        groupMemberRepository.delete(adminMember);

        // Notify all remaining group members via WebSocket
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        List<GroupMember> members = groupMemberRepository.findByGroup_GroupIdWithUser(groupId);
        GroupDTO groupDTO = applicationMapper.toGroupDTO(group, groupMemberRepository.countByGroupGroupId(groupId));
        for (GroupMember member : members) {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(member.getUser().getId()),
                    "/topic/admin-left",
                    groupDTO
            );
        }

        // Notify the new admin specifically
        messagingTemplate.convertAndSendToUser(
                String.valueOf(nextAdmin.getUser().getId()),
                "/topic/role-updated",
                groupDTO
        );

        AdminLeaveResult result = new AdminLeaveResult();
        result.setSuccess(true);
        result.setAction("leave");
        result.setGroupId(groupId);
        result.setNewAdminId(String.valueOf(nextAdmin.getUser().getId()));
        return result;
    }

    @Override
    public boolean leaveGroup(String userId, UUID groupId) {
        // Check if user is a member and their role
        List<GroupMemberDTO> members = getGroupMembers(userId, groupId);
        GroupMemberDTO currentMember = members.stream()
                .filter(m -> m.getUserId().equals(userId))
                .findFirst()
                .orElse(null);

        if (currentMember == null) {
            return false; // User is not a member
        }

        if (currentMember.getRole() == GroupRole.ADMIN) {
            throw new AccessDeniedException("Admins must use the admin-leave endpoint to leave groups");
        }

        // Regular leave for non-admin members
        return removeUserFromGroup(userId, groupId, userId);
    }
}
