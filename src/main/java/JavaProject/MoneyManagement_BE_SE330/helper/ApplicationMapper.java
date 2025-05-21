package JavaProject.MoneyManagement_BE_SE330.helper;

import JavaProject.MoneyManagement_BE_SE330.models.dtos.auth.RegisterDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.category.*;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.transaction.CreateTransactionDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.transaction.TransactionDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.transaction.UpdateTransactionDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.wallet.CreateWalletDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.wallet.WalletDTO;
import JavaProject.MoneyManagement_BE_SE330.models.entities.Category;
import JavaProject.MoneyManagement_BE_SE330.models.entities.Transaction;
import JavaProject.MoneyManagement_BE_SE330.models.entities.User;
import JavaProject.MoneyManagement_BE_SE330.models.entities.Wallet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

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
    User toUserEntity(RegisterDTO dto);
    default Set<String> createDefaultRoles() {
        return Set.of("USER");
    }

    // Category Mappings
    CategoryDTO toCategoryDTO(Category model);

    @Mapping(target = "categoryId", ignore = true)
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

    @Mapping(target = "walletId", source = "wallet", qualifiedByName = "extractWalletId")
    @Mapping(target = "categoryId", source = "category", qualifiedByName = "extractCategoryId")
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
    @Mapping(target = "wallet", source = "walletId", qualifiedByName = "mapWalletFromId")
    @Mapping(target = "category", source = "categoryId", qualifiedByName = "mapCategoryFromId")
    Transaction toTransactionEntity(CreateTransactionDTO model);

    @Mapping(target = "transactionId", ignore = true) // keep existing ID
    @Mapping(target = "wallet", source = "walletId", qualifiedByName = "mapWalletFromId")
    @Mapping(target = "category", source = "categoryId", qualifiedByName = "mapCategoryFromId")
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
}
