# RBAC System — Development Progress

> Stack: Spring Boot 4 · Java 21 · PostgreSQL · Hibernate (Schema-per-Tenant) · Flyway · Spring Security

---

## ✅ Foundation & Infrastructure

- [x] Spring Boot project setup (pom.xml, application.yaml, .env)
- [x] Multi-tenancy: `TenantContext` (ThreadLocal schema switcher)
- [x] Multi-tenancy: `TenantResolverFilter` (reads `X-Tenant-ID` header)
- [x] Multi-tenancy: `CurrentTenantIdentifierResolverImpl`
- [x] Multi-tenancy: `MultiTenantConnectionProviderImpl` (`SET search_path`)
- [x] Multi-tenancy: `TenantMigrationService` (programmatic Flyway for new tenants)
- [x] `HibernateMultiTenancyConfig` (custom EMF, wires multitenancy beans)
- [x] `PublicFlywayConfig` (explicit Flyway bean for public schema)
- [x] `SecurityConfig` (stateless, JWT-ready, `PasswordEncoder` bean)
- [x] `GlobalExceptionHandler` (validation, conflict, not-found, runtime, catch-all)
- [x] `ResponseBuilder` / `ResponseStructure` utility (standard response wrapper)
- [x] Lombok annotation processing fix (`pom.xml` `annotationProcessorPaths`)

---

## ✅ Database Migrations

### Public Schema (`classpath:db/migration/public/`)
- [x] `V1__init_public.sql` — `tenant_registry`, `roles`, `modules`, `actions`, `root_user` + seed data
- [x] `V2__update_root_user.sql` — aligns `root_user` columns
- [x] `V3__role_action_module_permission_table.sql` — `role_permissions` join table + indexes

### Tenant Schema (`classpath:db/migration/tenant/`)
- [x] `V1__init_tenant.sql` — `roles`, `modules`, `actions`, `role_permissions` + indexes

---

## ✅ Root User Module (`modules/root`)

- [x] `RootUser` entity → `public.root_user`
- [x] `RootUserRepository`
- [x] `RootUserProperties` (`@ConfigurationProperties` for `root.*` env vars)
- [x] `RootUserInitializer` (`ApplicationRunner` — idempotent auto-create at startup)

---

## ✅ Tenant Module (`modules/tenant`)

- [x] `TenantRegistry` entity → `public.tenant_registry`
- [x] `TenantRegistryRepository`
- [x] `CreateTenantRequest` DTO
- [x] `TenantResponse` DTO
- [x] `TenantService` (schema creation + Flyway migration trigger)
- [x] `TenantController` → `POST /api/v1/tenants`

---

## ✅ Role Module (`modules/role`)

- [x] `Role` entity → `public.roles`
- [x] `Roles` enum (role name constants)
- [x] `RoleRepository`

---

## ✅ Module Module (`modules/module`)

- [x] `Module` entity → `public.modules`
- [x] `ModuleRepository`

---

## ✅ Action Module (`modules/action`)

- [x] `Action` entity → `public.actions`
- [x] `ActionRepository`

---

## ✅ Role Permission Module (`modules/rolePermission`)

- [x] `RolePermission` entity → `public.role_permissions`
- [x] `RolePermissionRepository` (find by role, check allowed, delete by role)
- [x] `AssignRolePermissionRequest` DTO
- [x] `RolePermissionResponse` DTO
- [x] `RolePermissionService` interface
- [x] `RolePermissionServiceImpl` (idempotent bulk assign, skip duplicates)
- [x] `RolePermissionController` → `POST /api/v1/role-permissions`, `GET /api/v1/role-permissions`
- [x] Postman collection (`postman/RolePermission.postman_collection.json`)

---

## 🔲 Authentication Module (`modules/auth`)

- [ ] `JwtUtil` — generate & validate JWT tokens
- [ ] `JwtAuthFilter` — validates Bearer token on every request
- [ ] Root user login → `POST /api/v1/auth/root/login`
- [ ] Tenant admin login → `POST /api/v1/auth/login`
- [ ] Logout / token invalidation
- [ ] Refresh token support

---

## 🔲 User Management Module (`modules/user`)

- [ ] `TenantUser` entity (tenant schema `users` table)
- [ ] `TenantUserRepository`
- [ ] Register user within tenant → `POST /api/v1/users/register`
- [ ] Get all users → `GET /api/v1/users`
- [ ] Get user by ID → `GET /api/v1/users/{id}`
- [ ] Update user → `PUT /api/v1/users/{id}`
- [ ] Deactivate user → `DELETE /api/v1/users/{id}`

---

## 🔲 User-Role Assignment Module (`modules/userRole`)

- [ ] `UserRole` join entity (tenant schema `users_roles` table)
- [ ] `UserRoleRepository`
- [ ] Assign role to user → `POST /api/v1/user-roles`
- [ ] Revoke role from user → `DELETE /api/v1/user-roles`
- [ ] Get roles for user → `GET /api/v1/user-roles?userId=`

---

## 🔲 RBAC Authorization

- [ ] `RbacPermissionEvaluator` (custom `PermissionEvaluator`)
- [ ] `@PreAuthorize("hasPermission(module, action)")` on controllers
- [ ] Permission check API → `GET /api/v1/auth/check-permission`

---

## 🔲 Branch Management Module (`modules/branch`)

- [ ] `Branch` entity + migration
- [ ] `BranchRepository`
- [ ] CRUD endpoints → `/api/v1/branches`

---

## 🔲 Product Management Module (`modules/product`)

- [ ] `Product` entity + migration
- [ ] `ProductRepository`
- [ ] CRUD endpoints → `/api/v1/products`

---

## 🔲 Tax Management Module (`modules/tax`)

- [ ] `Tax` entity + migration
- [ ] `TaxRepository`
- [ ] CRUD endpoints → `/api/v1/taxes`

---

## 🔲 Audit Logging

- [ ] `AuditLog` entity + migration
- [ ] `AuditLogRepository`
- [ ] AOP-based `@Audited` annotation
- [ ] Get audit logs → `GET /api/v1/audit`

---

## Progress

| Area | Done | Total |
|---|---|---|
| Infrastructure | 11 | 11 |
| DB Migrations | 4 | 4 |
| Root User | 4 | 4 |
| Tenant | 6 | 6 |
| Role / Module / Action | 5 | 5 |
| Role Permissions | 8 | 8 |
| **Auth** | **0** | **6** |
| **User Management** | **0** | **7** |
| **User-Role** | **0** | **5** |
| **RBAC Authorization** | **0** | **3** |
| **Branch / Product / Tax** | **0** | **9** |
| **Audit** | **0** | **4** |
| **TOTAL** | **38** | **72** |
