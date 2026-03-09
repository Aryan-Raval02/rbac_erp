# Sign-Up API — Flow Analysis Report

**Endpoint:** `POST /api/v1/auth/signup`  
**Controller:** `GlobalUserController` → `GlobalUserService` → `GlobalUserServiceImpl`

---

## ✅ Full Execution Flow (As-Designed)

```
POST /api/v1/auth/signup
        │
        ▼
GlobalUserController.signUp()
        │
        ▼
GlobalUserServiceImpl.signUp()
  ├─ TenantContext = "public"
  └─ doSignUp()
        │
        ├─ 1. generateTenantSchemaName(companyName)   → e.g. "ACM_SER_91B2D0"
        ├─ 2. Duplicate check: tenant_registry        → TenantFoundException if exists
        ├─ 3. Duplicate check: global_users email     → CeoFoundException if exists
        ├─ 4. Duplicate check: global_users username  → CeoFoundException if exists
        ├─ 5. Save GlobalUser → public.global_users   ✅
        └─ 6. tenantService.createTenantSubscription(schemaName, companyName)
                  │
                  ├─ Duplicate schema guard (second check)
                  ├─ migrateTenant(schemaName)
                  │     ├─ sanitize(schema)
                  │     ├─ CREATE SCHEMA IF NOT EXISTS acm_ser_91b2d0
                  │     └─ Flyway: V1, V2, V3, V4 migrations
                  ├─ saveRegistry() → public.tenant_registry
                  ├─ globalUserRepository.findByTargetSchema(schemaName)
                  ├─ TenantContext.setCurrentTenant(schemaName)   ← switch to tenant
                  └─ seedCeoDetailsIntoTenant(ceos)
                        ├─ roleRepository.findByName("CEO")       ← ❌ BUG #1
                        ├─ userRepository.save(user)              → tenant.users
                        ├─ rolePermissionRepository.findByRoleId()
                        └─ userPermissionRepository.saveAll()      ← ❌ BUG #2
```

---

## 🐛 Bugs Found

### ❌ BUG #1 — `seedCeoDetailsIntoTenant` looks up role `"CEO"` — it does NOT exist in tenant schema

**Location:** `TenantService.java` line 158

```java
Role role = roleRepository.findByName("CEO")
        .orElseThrow(() -> new RoleNotFoundException("Role Not Found !!"));
```

**V2__seed_master_data.sql** copies roles from `public.roles` into the tenant schema.  
Looking at `V1__init_public.sql`, the public roles seeded are:

```
CEO, BRANCH_MANAGER, BRANCH_IN_CHARGE, TECHNICIAN_MANAGER,
TECHNICIAN, ACCOUNT_MANAGER, ACCOUNT_PERSON, SALES_MANAGER,
SALES_PERSON, HR, OPERATION_HEAD
```

✅ `"CEO"` **does** exist in `public.roles` and is copied to the tenant via `V2`.

> ⚠️ BUT — `roleRepository` uses the **current tenant's search_path**.  
> At the time `seedCeoDetailsIntoTenant()` is called, `TenantContext` is set to the new schema. The `V2` migration runs **inside Flyway's own JDBC connection** — not the Hibernate connection. Hibernate's `search_path` is only set when it borrows a connection. There is a race window between Flyway finishing and Hibernate seeing the seeded data.  
> **This is likely safe in practice** but worth being aware of.

---

### ❌ BUG #2 — `UserPermission` built without `allowed` — will throw `NOT NULL` constraint violation

**Location:** `TenantService.java` lines 227–232 (and `TenantMigrationService.java` lines 227–232 — the duplicate)

```java
userPermissions.add(UserPermission.builder()
        .action(rp.getAction())
        .module(rp.getModule())
        .user(saved)
        // ← allowed is NOT SET
        .build()
);
```

**Entity:**
```java
@Column(nullable = false)
private Boolean allowed;    // ← no @Builder.Default, no default value
```

When `allowed` is not set in the builder, it will be `null`. Trying to save this to the DB will throw:

```
PSQLException: ERROR: null value in column "allowed" of relation "user_permissions"
violates not-null constraint
```

**Fix required:** Add `.allowed(true)` to the builder.

---

### ⚠️ BUG #3 — `seedCeoDetailsIntoTenant` is DUPLICATED in two classes

The exact same private method exists in **both**:
- `TenantService.java` (lines 154–196) ← **this one is actually called**
- `TenantMigrationService.java` (lines 204–237) ← **dead code, never called**

`TenantMigrationService.seedCeoDetailsIntoTenant` is dead code. Both copies have BUG #2.

---

### ⚠️ BUG #4 — `@Transactional` on `signUp()` conflicts with Flyway DDL

**Location:** `GlobalUserServiceImpl.java` line 58

```java
@Override
@Transactional
public SignUpResponse signUp(SignUpRequest request) {
    TenantContext.setCurrentTenant(TenantContext.DEFAULT_TENANT);
    try {
        return doSignUp(request);   // ← calls createTenantSubscription → Flyway DDL inside
    } finally {
        TenantContext.clear();
    }
}
```

`migrateTenant()` runs DDL via its own raw JDBC connection (correctly using autocommit=true for CREATE SCHEMA), but the `@Transactional` wrapper on `signUp()` means the JPA session and the Flyway JDBC connection operate in different transaction contexts. If the outer JPA `@Transactional` rolls back for any reason **after** Flyway finishes, the schema and tenant_registry row will be in an inconsistent state (schema exists but no registry row).

> The `TenantService.saveRegistry()` is `@Transactional(REQUIRES_NEW)` which partially protects this, but the outer transaction rollback scenario is still a risk.

---

## Summary Table

| # | Severity | Bug | Where | Impact |
|---|---|---|---|---|
| 1 | 🟡 Low risk | `"CEO"` role lookup — valid but timing-sensitive | `TenantService.seedCeoDetailsIntoTenant` | Potential race on fresh schema |
| 2 | 🔴 **CRASH** | `UserPermission` built without `allowed` | `TenantService` + `TenantMigrationService` | `NOT NULL` DB constraint violation every time |
| 3 | 🟡 Code quality | `seedCeoDetailsIntoTenant` duplicated in 2 classes | `TenantMigrationService` (dead copy) | Dead code confusion |
| 4 | 🟠 Medium risk | `@Transactional` outer wrap + inner Flyway DDL | `GlobalUserServiceImpl.signUp()` | Inconsistent state on rollback |

---

## What Currently Works ✅

| Step | Status | Notes |
|---|---|---|
| Schema name generation | ✅ | CRC32 hash + initials — deterministic, PG-safe |
| Duplicate guards (email, username, schema) | ✅ | All 3 checked |
| Save to `public.global_users` | ✅ | Password BCrypt-hashed |
| `CREATE SCHEMA` | ✅ | Unquoted, autocommit-safe |
| Flyway V1–V4 migrations | ✅ | Roles/modules/actions/permissions/users/user_permissions tables created |
| V2 seed data copy from public | ✅ | Roles, modules, actions, role_permissions copied with IDs preserved |
| Save `public.tenant_registry` | ✅ | REQUIRES_NEW transaction |
| Find CEO by schema from global_users | ✅ | `findByTargetSchema()` query |
| Save CEO to tenant `users` table | ✅ | Role resolved, all fields populated |
| Save `user_permissions` | ❌ | **Crashes — `allowed` is null** |
