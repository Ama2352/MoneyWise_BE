package JavaProject.MoneyManagement_BE_SE330.helper;

import JavaProject.MoneyManagement_BE_SE330.models.dtos.auth.RegisterDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.category.*;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.group.CreateGroupDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.group.GroupDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.group.GroupMemberDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.profile.UpdateProfileDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.profile.UserProfileDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.transaction.CreateTransactionDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.transaction.TransactionDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.transaction.TransactionDetailDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.transaction.UpdateTransactionDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.wallet.CreateWalletDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.wallet.WalletDTO;
import JavaProject.MoneyManagement_BE_SE330.models.entities.*;
import JavaProject.MoneyManagement_BE_SE330.models.enums.GroupRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mapper(componentModel = "spring", uses = PasswordEncoderHelper.class)
public interface ApplicationMapper {

    // Map RegisterDTO to User
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", source = "email") // set username = email
    @Mapping(target = "password", source = "password", qualifiedByName = "encode")
    @Mapping(target = "roles", expression = "java(createDefaultRoles())")
    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "wallets", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "friendRequestsSent", ignore = true)
    @Mapping(target = "friendRequestsReceived", ignore = true)
    @Mapping(target = "messagesSent", ignore = true)
    @Mapping(target = "messagesReceived", ignore = true)
    @Mapping(target = "createdGroups", ignore = true)
    @Mapping(target = "groupMemberships", ignore = true)
    User toUserEntity(RegisterDTO dto);
    default Set<String> createDefaultRoles() {
        return Set.of("USER");
    }

    // Category Mappings
    CategoryDTO toCategoryDTO(Category model);

    @Mapping(target = "categoryID", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    Category toCategoryEntity(CreateCategoryDTO model);

    // Wallet Mappings
    WalletDTO toWalletDTO(Wallet model);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    Wallet toWalletEntity(CreateWalletDTO model);

    // Transaction Mappings
    @Mapping(target = "walletID", source = "wallet", qualifiedByName = "extractWalletID")
    @Mapping(target = "categoryID", source = "category", qualifiedByName = "extractCategoryID")
    TransactionDTO toTransactionDTO(Transaction model);

    @Named("extractWalletID")
    default UUID extractWalletID(Wallet wallet) {
        return wallet.getWalletID();
    }
    @Named("extractCategoryID")
    default UUID extractCategoryID(Category category) {
        return category.getCategoryID();
    }

    @Mapping(target = "transactionID", ignore = true) // generated on persist
    @Mapping(target = "wallet", source = "walletID", qualifiedByName = "mapWalletFromID")
    @Mapping(target = "category", source = "categoryID", qualifiedByName = "mapCategoryFromID")
    Transaction toTransactionEntity(CreateTransactionDTO model);

    @Mapping(target = "transactionID", ignore = true) // keep existing ID
    @Mapping(target = "wallet", source = "walletID", qualifiedByName = "mapWalletFromID")
    @Mapping(target = "category", source = "categoryID", qualifiedByName = "mapCategoryFromID")
    Transaction toTransactionEntity(UpdateTransactionDTO model);

    // Helper mappings to convert UUIDs to entities with only IDs set (for reference)
    @Named("mapWalletFromID")
    default Wallet mapWalletFromID(UUID walletID) {
        if (walletID == null) {
            return null;
        }
        Wallet wallet = new Wallet();
        wallet.setWalletID(walletID);
        return wallet;
    }

    @Named("mapCategoryFromID")
    default Category mapCategoryFromID(UUID categoryID) {
        if (categoryID == null) {
            return null;
        }
        Category category = new Category();
        category.setCategoryID(categoryID);
        return category;
    }

    // ----------- Transaction → TransactionDetailDTO Mapping -----------
    @Mapping(target = "date", expression = "java(transaction.getTransactionDate())")
    @Mapping(target = "time", expression = "java(transaction.getTransactionDate().toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern(\"HH:mm:ss\")))")
    @Mapping(target = "dayOfWeek", expression = "java(capitalizeDayOfWeek(transaction.getTransactionDate().getDayOfWeek()))")
    @Mapping(target = "month", expression = "java(transaction.getTransactionDate().getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH))")
    @Mapping(target = "category", source = "category.name")
    @Mapping(target = "categoryID", source = "category", qualifiedByName = "extractCategoryID")
    @Mapping(target = "walletID", source = "wallet", qualifiedByName = "extractWalletID")
    @Mapping(target = "walletName", source = "wallet.walletName")
    TransactionDetailDTO toTransactionDetailDTO(Transaction transaction);

    default String capitalizeDayOfWeek(DayOfWeek dayOfWeek) {
        String lower = dayOfWeek.toString().toLowerCase(); // e.g. "monday"
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1); // "Monday"
    }

    // Map User to UserProfileDTO
    @Mapping(target = "id", expression = "java(user.getId().toString())")
    @Mapping(target = "displayName", source = "user", qualifiedByName = "fullName")
    UserProfileDTO toUserProfileDTO(User user);
    
    // Mapping for group
    @Mapping(target = "groupId", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "creator", ignore = true)
    @Mapping(target = "members", ignore = true)
    Group toGroupEntity(CreateGroupDTO dto);

    @Mapping(target = "creatorId", expression = "java(group.getCreator().getId().toString())")
    @Mapping(target = "creatorName", source = "group.creator", qualifiedByName = "fullName")
    @Mapping(target = "role", expression = "java(JavaProject.MoneyManagement_BE_SE330.models.enums.GroupRole.ADMIN)")
    @Mapping(target = "memberCount", source = "memberCount")
    GroupDTO toGroupDTO(Group group, int memberCount);

    @Named("fullName")
    default String mapFullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    @Mapping(target = "userId", expression = "java(model.getUser().getId().toString())")
    @Mapping(target = "displayName", source = "model.user", qualifiedByName = "fullName")
    @Mapping(target = "avatarUrl", expression = "java(model.getUser().getAvatarUrl())")
    GroupMemberDTO toGroupMemberDTO(GroupMember model);

}
