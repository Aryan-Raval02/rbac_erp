package com.security.rbac.modules.tenant.dto;

import com.security.rbac.modules.tenant.entity.TenantRegistry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TenantRegistryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "schemaName", ignore = true) // Handled in Service
    @Mapping(target = "status", ignore = true)     // Default is ACTIVE
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TenantRegistry toEntity(CreateTenantRequest request);

    TenantResponse toResponse(TenantRegistry entity);

    List<TenantResponse> toResponseList(List<TenantRegistry> entities);
}
