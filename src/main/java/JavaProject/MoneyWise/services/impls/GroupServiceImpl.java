package JavaProject.MoneyWise.services.impls;

import JavaProject.MoneyWise.helper.ApplicationMapper;
import JavaProject.MoneyWise.helper.HelperFunctions;
import JavaProject.MoneyWise.helper.ResourceNotFoundException;
import JavaProject.MoneyWise.models.dtos.group.*;
import JavaProject.MoneyWise.models.entities.Group;
import JavaProject.MoneyWise.models.entities.GroupMember;
import JavaProject.MoneyWise.models.entities.User;
import JavaProject.MoneyWise.models.enums.GroupRole;
import JavaProject.MoneyWise.repositories.GroupMemberRepository;
import JavaProject.MoneyWise.repositories.GroupRepository;
import JavaProject.MoneyWise.repositories.UserRepository;
import JavaProject.MoneyWise.services.GroupService;
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
        newGroup.setCreator(HelperFunctions.getCurrentUser(userRepository));
        groupRepository.save(newGroup);

        // Add creator as first admin member
        GroupMember creatorMember = new GroupMember();
        creatorMember.setGroup(newGroup);
        UUID creatorUuid = UUID.fromString(creatorId);
        User creator = userRepository.findById(creatorUuid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        creatorMember.setUser(creator);
        creatorMember.setRole(GroupRole.ADMIN);
        groupMemberRepository.save(creatorMember);

        // Add initial members if provided
        if (dto.getInitialMemberIds() != null && !dto.getInitialMemberIds().isEmpty()) {
            for (String memberId : dto.getInitialMemberIds()) {
                if (!memberId.equals(creatorId)) {
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
        if (dto.getInitialMemberIds() != null) {
            memberIds.addAll(dto.getInitialMemberIds());
        }
        for (String memberId : memberIds) {
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
        UUID userUuid = UUID.fromString(userId);

        // Get all GroupMember entities for this user
        List<GroupMember> groupMembers = groupMemberRepository.findAllByUserId(userUuid);

        List<GroupDTO> groups = new ArrayList<>();

        for (GroupMember member : groupMembers) {
            Group group = member.getGroup();

            // Map Group entity to GroupDTO
            GroupDTO groupDTO = applicationMapper.toGroupDTO(group, 0);

            // Set creator full name
            User creator = group.getCreator();
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
        UUID userUuid = UUID.fromString(userId);

        // Check if user is a member of the group
        boolean isMember = groupMemberRepository.existsByGroup_GroupIdAndUser_Id(groupId, userUuid);
        if (!isMember) {
            throw new AccessDeniedException("User is not a member of this group");
        }

        // Get all members of the group
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
        // Verify the current user is an admin or collaborator
        UUID adminUuid = UUID.fromString(adminUserId);
        GroupMember adminMember = groupMemberRepository.findByGroup_GroupIdAndUser_Id(groupId, adminUuid)
                .orElseThrow(() -> new AccessDeniedException("Only group admins or collaborators can add members"));

        if (adminMember.getRole() != GroupRole.ADMIN && adminMember.getRole() != GroupRole.COLLABORATOR) {
            throw new AccessDeniedException("Only group admins or collaborators can add members");
        }

        // Check if user is already a member
        UUID newUserUuid = UUID.fromString(newUserId);
        boolean isAlreadyMember = groupMemberRepository.existsByGroup_GroupIdAndUser_Id(groupId, newUserUuid);
        if (isAlreadyMember) {
            return false;
        }

        // Verify group exists
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        // Verify new user exists
        User newUser = userRepository.findById(newUserUuid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Add the new member
        GroupMember newMember = new GroupMember();
        newMember.setGroup(group);
        newMember.setUser(newUser);
        newMember.setRole(GroupRole.MEMBER);
        groupMemberRepository.save(newMember);

        // Notify the new member
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
        boolean isSelfRemoval = adminUserId.equals(userToRemoveId);

        // Find the membership to remove
        UUID userToRemoveUuid = UUID.fromString(userToRemoveId);
        Optional<GroupMember> memberToRemoveOpt = groupMemberRepository.findByGroup_GroupIdAndUser_Id(groupId, userToRemoveUuid);
        if (memberToRemoveOpt.isEmpty()) {
            return false;
        }
        GroupMember memberToRemove = memberToRemoveOpt.get();

        // If not self-removal, check if admin
        if (!isSelfRemoval) {
            UUID adminUuid = UUID.fromString(adminUserId);
            GroupMember adminMember = groupMemberRepository.findByGroup_GroupIdAndUser_Id(groupId, adminUuid)
                    .orElseThrow(() -> new AccessDeniedException("Only group admins or collaborators can remove members"));

            if (adminMember.getRole() != GroupRole.ADMIN && adminMember.getRole() != GroupRole.COLLABORATOR) {
                throw new AccessDeniedException("Only group admins or collaborators can remove members");
            }

            // Collaborators can't remove admins
            if (adminMember.getRole() == GroupRole.COLLABORATOR && memberToRemove.getRole() == GroupRole.ADMIN) {
                throw new AccessDeniedException("Collaborators cannot remove admins");
            }
        }

        // Check if removing the last admin (self-removal)
        if (memberToRemove.getRole() == GroupRole.ADMIN && isSelfRemoval) {
            long adminCount = groupMemberRepository.countByGroupGroupIdAndRole(groupId, GroupRole.ADMIN);
            if (adminCount <= 1) {
                throw new IllegalStateException("As the last admin, you must use the admin leave process");
            }
        }

        // Remove the member
        groupMemberRepository.delete(memberToRemove);

        // Notify the removed user
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
        // Verify the current user is an admin
        UUID adminUuid = UUID.fromString(adminUserId);
        boolean isAdmin = groupMemberRepository.existsByGroupGroupIdAndUserIdAndRole(groupId, adminUuid, GroupRole.ADMIN);
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

        // Notify all group members
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
        // Verify the current user is an admin
        UUID adminUuid = UUID.fromString(adminUserId);
        GroupMember adminMember = groupMemberRepository.findByGroup_GroupIdAndUser_Id(groupId, adminUuid)
                .orElseThrow(() -> new AccessDeniedException("Only group admins can assign roles"));

        if (adminMember.getRole() != GroupRole.ADMIN) {
            throw new AccessDeniedException("Only group admins can assign roles");
        }

        // Find the member to promote
        UUID userUuid = UUID.fromString(userId);
        GroupMember member = groupMemberRepository.findByGroup_GroupIdAndUser_Id(groupId, userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("User " + userId + " is not a member of this group"));

        // Assign collaborator role
        member.setRole(GroupRole.COLLABORATOR);
        groupMemberRepository.save(member);

        // Notify the promoted user
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
        // Verify the current user is an admin
        UUID adminUuid = UUID.fromString(adminUserId);
        GroupMember adminMember = groupMemberRepository.findByGroup_GroupIdAndUser_Id(groupId, adminUuid)
                .orElseThrow(() -> new AccessDeniedException("Only group admins can delete groups"));

        if (adminMember.getRole() != GroupRole.ADMIN) {
            throw new AccessDeniedException("Only group admins can delete groups");
        }

        // Find the group with members
        Group group = groupRepository.findByIdWithMembers(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group with ID " + groupId + " not found"));

        // Notify all group members before deletion
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
        UUID adminUuid = UUID.fromString(adminUserId);
        GroupMember adminMember = groupMemberRepository.findByGroup_GroupIdAndUser_Id(groupId, adminUuid)
                .orElseThrow(() -> new ResourceNotFoundException("User is not a member of this group"));

        if (adminMember.getRole() != GroupRole.ADMIN) {
            throw new AccessDeniedException("Only admins can use this function");
        }

        // If delete option is selected, delete the group
        if (deleteGroup) {
            boolean deleted = deleteGroup(adminUserId, groupId);
            AdminLeaveResult result = new AdminLeaveResult();
            result.setSuccess(deleted);
            result.setAction("delete");
            result.setGroupId(groupId);
            return result;
        }

        // Find next admin
        GroupMember nextAdmin = groupMemberRepository.findNextAdmin(groupId, adminUuid)
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

        // Remove the current admin
        groupMemberRepository.delete(adminMember);

        // Notify all remaining group members
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

        // Notify the new admin
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

    @Transactional
    @Override
    public boolean leaveGroup(String userId, UUID groupId) {
        // Check if user is a member and their role
        List<GroupMemberDTO> members = getGroupMembers(userId, groupId);
        GroupMemberDTO currentMember = members.stream()
                .filter(m -> m.getUserId().equals(userId))
                .findFirst()
                .orElse(null);

        if (currentMember == null) {
            return false;
        }

        if (currentMember.getRole() == GroupRole.ADMIN) {
            throw new AccessDeniedException("Admins must use the admin-leave endpoint to leave groups");
        }

        // Regular leave for non-admin members
        return removeUserFromGroup(userId, groupId, userId);
    }
}