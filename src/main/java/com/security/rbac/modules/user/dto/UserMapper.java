package com.security.rbac.modules.user.dto;

import com.security.rbac.modules.user.dto.request.CreateUserRequest;
import com.security.rbac.modules.user.dto.response.UserResponse;
import com.security.rbac.modules.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", ignore = true) // Handled in Service layer due to DB lookup
    @Mapping(target = "reportingManager", ignore = true) // Handled in Service layer
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(CreateUserRequest request);

    @Mapping(source = "role.id", target = "roleId")
    @Mapping(source = "role.name", target = "roleName")
    @Mapping(source = "reportingManager.id", target = "reportingManagerId")
    UserResponse toResponse(User entity);

    List<UserResponse> toResponseList(List<User> entities);
}
