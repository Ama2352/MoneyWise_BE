package JavaProject.MoneyManagement_BE_SE330.helper;

import JavaProject.MoneyManagement_BE_SE330.models.dtos.RegisterDTO;
import JavaProject.MoneyManagement_BE_SE330.models.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;

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

}
