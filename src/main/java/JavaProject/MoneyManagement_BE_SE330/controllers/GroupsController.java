package JavaProject.MoneyManagement_BE_SE330.controllers;

import JavaProject.MoneyManagement_BE_SE330.helper.ResourceNotFoundException;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.group.CreateGroupDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.group.GroupDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.group.GroupMemberDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.group.UpdateGroupDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.group.AdminLeaveResult;
import JavaProject.MoneyManagement_BE_SE330.models.entities.User;
import JavaProject.MoneyManagement_BE_SE330.services.GroupService;
import JavaProject.MoneyManagement_BE_SE330.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/Groups")
@Tag(name = "Groups")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class GroupsController {

    private final GroupService groupService;
    private final UserService userService;
    private final Logger logger = LoggerFactory.getLogger(GroupsController.class);

    @PostMapping
    @Operation(
        summary = "Creates a new group chat",
        description = "Group creation data including name and optional member list"
    )
    public ResponseEntity<GroupDTO> createGroup(@RequestBody @Valid CreateGroupDTO dto) {
        User currentUser = userService.getCurrentUser();
        GroupDTO group = groupService.createGroup(String.valueOf(currentUser.getId()), dto);
        return ResponseEntity.ok(group);
    }

    @GetMapping
    @Operation(summary = "Gets all groups the current user is a member of")
    public ResponseEntity<List<GroupDTO>> getUserGroups() {
        User currentUser = userService.getCurrentUser();
        List<GroupDTO> groups = groupService.getUserGroups(String.valueOf(currentUser.getId()));
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/{groupId}/members")
    @Operation(summary = "Gets all members of a group")
    public ResponseEntity<List<GroupMemberDTO>> getGroupMembers(@PathVariable UUID groupId) {
        User currentUser = userService.getCurrentUser();
        try {
            List<GroupMemberDTO> members = groupService.getGroupMembers(String.valueOf(currentUser.getId()), groupId);
            return ResponseEntity.ok(members);
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không phải là thành viên của nhóm này.");
        }
    }

    @PostMapping("/{groupId}/members/{userId}")
    @Operation(summary = "Adds a user to a group (admin only)")
    public ResponseEntity<Map<String, Boolean>> addUserToGroup(@PathVariable UUID groupId, @PathVariable String userId) {
        User currentUser = userService.getCurrentUser();
        try {
            boolean result = groupService.addUserToGroup(String.valueOf(currentUser.getId()), groupId, userId);
            return ResponseEntity.ok(Map.of("success", result));
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ quản trị viên hoặc người cộng tác mới có thể thêm thành viên.");
        }
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    @Operation(summary = "Removes a user from a group (admin only, or self-removal)")
    public ResponseEntity<Map<String, Boolean>> removeUserFromGroup(@PathVariable UUID groupId, @PathVariable String userId) {
        User currentUser = userService.getCurrentUser();
        try {
            boolean result = groupService.removeUserFromGroup(String.valueOf(currentUser.getId()), groupId, userId);
            return ResponseEntity.ok(Map.of("success", result));
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xóa thành viên khỏi nhóm này.");
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PutMapping("/{groupId}")
    @Operation(summary = "Updates group information (admin only)")
    public ResponseEntity<Map<String, Boolean>> updateGroup(@PathVariable UUID groupId, @RequestBody @Valid UpdateGroupDTO dto) {
        User currentUser = userService.getCurrentUser();
        try {
            boolean result = groupService.updateGroup(String.valueOf(currentUser.getId()), groupId, dto);
            return ResponseEntity.ok(Map.of("success", result));
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ quản trị viên nhóm mới có thể cập nhật thông tin nhóm.");
        } catch (ResourceNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm.");
        }
    }

    @PostMapping("/{groupId}/admin-leave")
    @Operation(summary = "Allows an admin to leave a group with option to delete")
    public ResponseEntity<AdminLeaveResult> adminLeaveGroup(@PathVariable UUID groupId, @RequestParam(defaultValue = "false") boolean deleteGroup) {
        User currentUser = userService.getCurrentUser();
        try {
            AdminLeaveResult result = groupService.adminLeaveGroup(String.valueOf(currentUser.getId()), groupId, deleteGroup);
            return ResponseEntity.ok(result);
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (ResourceNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping("/{groupId}/members/{userId}/collaborator")
    @Operation(summary = "Assigns collaborator role to a group member")
    public ResponseEntity<Map<String, Boolean>> assignCollaboratorRole(@PathVariable UUID groupId, @PathVariable String userId) {
        User currentUser = userService.getCurrentUser();
        try {
            boolean result = groupService.assignCollaboratorRole(String.valueOf(currentUser.getId()), groupId, userId);
            return ResponseEntity.ok(Map.of("success", result));
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ quản trị viên nhóm mới có thể chỉ định người cộng tác.");
        } catch (ResourceNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng trong nhóm.");
        }
    }

    @PostMapping("/{groupId}/leave")
    @Operation(summary = "Allows a user to leave a group")
    public ResponseEntity<Map<String, Boolean>> leaveGroup(@PathVariable UUID groupId) {
        User currentUser = userService.getCurrentUser();
        try {
            boolean result = groupService.leaveGroup(String.valueOf(currentUser.getId()), groupId);
            if (result) {
                logger.info("User {} left group {}", currentUser.getId(), groupId);
                return ResponseEntity.ok(Map.of("success", true));
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found or user is not a member");
            }
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admins must use the admin-leave endpoint to leave groups");
        }
    }
}