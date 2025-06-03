package JavaProject.MoneyManagement_BE_SE330.helper;

import JavaProject.MoneyManagement_BE_SE330.models.dtos.auth.RegisterDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.budget.BudgetDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.budget.BudgetProgressDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.budget.CreateBudgetDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.budget.UpdateBudgetDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.category.*;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.group.CreateGroupDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.group.GroupDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.group.GroupMemberDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.profile.UserProfileDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.savingGoal.CreateSavingGoalDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.savingGoal.SavingGoalDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.savingGoal.SavingGoalProgressDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.transaction.CreateTransactionDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.transaction.TransactionDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.transaction.TransactionDetailDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.transaction.UpdateTransactionDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.wallet.CreateWalletDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.wallet.WalletDTO;
import JavaProject.MoneyManagement_BE_SE330.models.entities.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.DayOfWeek;
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
    @Mapping(target = "categoryID", source = "categoryId")
    CategoryDTO toCategoryDTO(Category model);

    @Mapping(target = "categoryId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    Category toCategoryEntity(CreateCategoryDTO model);

    // Wallet Mappings
    @Mapping(target = "walletID", source = "walletId")
    WalletDTO toWalletDTO(Wallet model);

    @Mapping(target = "walletId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    Wallet toWalletEntity(CreateWalletDTO model);

    // Transaction Mappings
    @Mapping(target = "transactionID", source = "transactionId")
    @Mapping(target = "walletID", source = "wallet", qualifiedByName = "extractWalletId")
    @Mapping(target = "categoryID", source = "category", qualifiedByName = "extractCategoryId")
    TransactionDTO toTransactionDTO(Transaction model);

    @Named("extractWalletId")
    default UUID extractWalletId(Wallet wallet) {
        return wallet.getWalletId();
    }
    @Named("extractCategoryId")
    default UUID extractCategoryId(Category category) {
        return category.getCategoryId();
    }

    @Mapping(target = "transactionId", ignore = true) // generated on persist
    @Mapping(target = "wallet", source = "walletID", qualifiedByName = "mapWalletFromId")
    @Mapping(target = "category", source = "categoryID", qualifiedByName = "mapCategoryFromId")
    Transaction toTransactionEntity(CreateTransactionDTO model);

    @Mapping(target = "transactionId", ignore = true) // keep existing ID
    @Mapping(target = "wallet", source = "walletID", qualifiedByName = "mapWalletFromId")
    @Mapping(target = "category", source = "categoryID", qualifiedByName = "mapCategoryFromId")
    Transaction toTransactionEntity(UpdateTransactionDTO model);

    // Helper mappings to convert UUIDs to entities with only IDs set (for reference)
    @Named("mapWalletFromId")
    default Wallet mapWalletFromId(UUID walletId) {
        if (walletId == null) {
            return null;
        }
        Wallet wallet = new Wallet();
        wallet.setWalletId(walletId);
        return wallet;
    }

    @Named("mapCategoryFromId")
    default Category mapCategoryFromId(UUID categoryId) {
        if (categoryId == null) {
            return null;
        }
        Category category = new Category();
        category.setCategoryId(categoryId);
        return category;
    }

    // ----------- Transaction → TransactionDetailDTO Mapping -----------
    @Mapping(target = "date", expression = "java(transaction.getTransactionDate())")
    @Mapping(target = "time", expression = "java(transaction.getTransactionDate().toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern(\"HH:mm:ss\")))")
    @Mapping(target = "dayOfWeek", expression = "java(capitalizeDayOfWeek(transaction.getTransactionDate().getDayOfWeek()))")
    @Mapping(target = "month", expression = "java(transaction.getTransactionDate().getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH))")
    @Mapping(target = "category", source = "category.name")
    @Mapping(target = "categoryID", source = "category", qualifiedByName = "extractCategoryId")
    @Mapping(target = "walletID", source = "wallet", qualifiedByName = "extractWalletId")
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

    // Mapping for budget
    @Mapping(target = "categoryId", expression = "java(budget.getCategory().getCategoryId())")
    @Mapping(target = "walletId", expression = "java(budget.getWallet().getWalletId())")
    BudgetDTO toBudgetDTO(Budget budget);

    @Mapping(target = "categoryId", expression = "java(budget.getCategory().getCategoryId())")
    @Mapping(target = "walletId", expression = "java(budget.getWallet().getWalletId())")
    @Mapping(target = "usagePercentage", ignore = true)
    @Mapping(target = "progressStatus", ignore = true)
    @Mapping(target = "notification", ignore = true)
    BudgetProgressDTO toBudgetProgressDTO(Budget budget);

    @Mapping(target = "budgetId", ignore = true)
    @Mapping(target = "wallet", source = "walletId", qualifiedByName = "mapWalletFromId")
    @Mapping(target = "category", source = "categoryId", qualifiedByName = "mapCategoryFromId")
    @Mapping(target = "createdAt", ignore = true)
    Budget toBudgetEntity(CreateBudgetDTO dto);

    // Mapping for saving goal
    @Mapping(target = "categoryId", expression = "java(model.getCategory().getCategoryId())")
    @Mapping(target = "walletId", expression = "java(model.getWallet().getWalletId())")
    SavingGoalDTO toSavingGoalDTO(SavingGoal model);

    @Mapping(target = "categoryId", expression = "java(model.getCategory().getCategoryId())")
    @Mapping(target = "walletId", expression = "java(model.getWallet().getWalletId())")
    @Mapping(target = "savedPercentage", ignore = true)
    @Mapping(target = "progressStatus", ignore = true)
    @Mapping(target = "notification", ignore = true)
    SavingGoalProgressDTO toSavingGoalProgressDTO(SavingGoal model);

    @Mapping(target = "savingGoalId", ignore = true)
    @Mapping(target = "wallet", source = "walletId", qualifiedByName = "mapWalletFromId")
    @Mapping(target = "category", source = "categoryId", qualifiedByName = "mapCategoryFromId")
    @Mapping(target = "createdAt", ignore = true)
    SavingGoal toSavingGoalEntity(CreateSavingGoalDTO dto);
}
