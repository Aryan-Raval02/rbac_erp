# JWT Auth Implementation Reference — RBAC Project

> **Purpose:** Everything you need to know about the existing project before implementing the 3-type JWT login system.

---

## 1. The 3 Login Types

| # | User Type | Table | Schema | Password Field | Identifier |
|---|---|---|---|---|---|
| 1 | **Service/Root** | `public.root_user` | always `public` | `password` | `username` or `email` |
| 2 | **CEO (Global)** | `public.global_users` | always `public` | `password_hash` | `username` or `email` |
| 3 | **Tenant User** | `<schema>.users` | per-tenant (from `X-Tenant-ID` header) | `password_hash` | `username` or `email` |

---

## 2. Existing Entities — Auth-Relevant Fields

### 2a. `RootUser` → `public.root_user`
```
id, username (unique), email (unique), password (BCrypt),
name, role (Enum: Roles), active (boolean),
createdAt, updatedAt
```
- Package: `modules.root.entity.RootUser`
- Always public schema (`@Table(schema = "public")`)
- Role stored as `Roles` enum (STRING column name `role`)
- Created at startup by `RootUserInitializer` from env vars

### 2b. `GlobalUser` → `public.global_users`
```
id, email (unique), username (unique), passwordHash (BCrypt),
fullName, phoneNumber, targetSchema, systemRole ("CEO"),
isActive (boolean), lastLoginAt, createdAt, updatedAt
```
- Package: `modules.ceo.entity.GlobalUser`
- Always public schema (`@Table(schema = "public")`)
- **`targetSchema`** = the tenant schema this user owns (e.g., `acm_ser_91b2d0`) — **must go into the JWT claim**
- `systemRole` = hardcoded `"CEO"`

### 2c. `User` → `<tenantSchema>.users`
```
id, email (unique), username (unique), passwordHash (BCrypt),
fullName, phoneNumber, role (ManyToOne → Role), isActive (boolean),
lastLoginAt, createdAt, updatedAt
```
- Package: `modules.user.entity.User`
- **No `schema=` in `@Table`** — Hibernate uses tenant `search_path`
- `role.id` and `role.name` needed in JWT claims for RBAC
- Requires `X-Tenant-ID` header to resolve the correct schema

---

## 3. Existing Repositories

| Repository | Key Auth Methods Needed (add if missing) |
|---|---|
| `RootUserRepository` | `findByUsername(String)`, `findByEmail(String)` |
| `GlobalUserRepository` | `findByUsername(String)`, `findByEmail(String)` |
| `UserRepository` | `findByUsername(String)`, `findByEmail(String)` |

---

## 4. Existing Security Config (`SecurityConfig.java`)

```java
// Current state — everything under /api/v1/** is OPEN
.requestMatchers("/api/v1/**", "/swagger-ui/**", ...).permitAll()
.anyRequest().authenticated()

// SessionCreationPolicy.STATELESS  ← ✅ already set
// CSRF disabled                    ← ✅ already set
// BCryptPasswordEncoder bean        ← ✅ already set
// @EnableMethodSecurity(prePostEnabled = true) ← ✅ already set
```

**What to add:** `JwtAuthFilter` registered before `UsernamePasswordAuthenticationFilter`. Tighten `/api/v1/**` — only allow auth endpoints publicly.

---

## 5. Existing Filter Chain (`TenantResolverFilter`)

- Reads `X-Tenant-ID` header → sets `TenantContext.setCurrentTenant()`
- Already registered via `addFilterBefore(tenantResolverFilter, UsernamePasswordAuthenticationFilter.class)`
- JWT filter must also run in this chain (before auth filter)

---

## 6. `TenantContext` — Thread-Local Schema Store

```java
TenantContext.DEFAULT_TENANT = "public"
TenantContext.setCurrentTenant(String schema)
TenantContext.getCurrentTenant()   // returns "public" if not set
TenantContext.clear()
```

The JWT filter must extract `tenantSchema` from the token and call `TenantContext.setCurrentTenant()` for tenant user requests.

---

## 7. RBAC Authorization Tables (Tenant Schema)

```
roles             (id, name, description)
modules           (id, name, description)
actions           (id, name, description)
role_permissions  (role_id, module_id, action_id, allowed)
user_permissions  (user_id, module_id, action_id, allowed)  ← overrides role
```

For authorization, user permissions **override** role permissions:
- `user_permissions.allowed = true`  → always granted (overrides role deny)
- `user_permissions.allowed = false` → always denied (overrides role grant)
- No user_permission row → fall back to role_permissions

---

## 8. What Needs to Be Added

### 8a. JWT Dependency (not in pom.xml yet)
```xml
<!-- JJWT library — add to pom.xml -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

### 8b. application.yaml — JWT Config to Add
```yaml
app:
  jwt:
    secret: "<256-bit base64 secret>"
    expiration-ms: 86400000        # 24 hours
    refresh-expiration-ms: 604800000  # 7 days
```

### 8c. Login Endpoints to Create

| Endpoint | User Type | Request Body | Notes |
|---|---|---|---|
| `POST /api/v1/auth/root/login` | Root/Service | `{username, password}` | No tenant header needed |
| `POST /api/v1/auth/ceo/login` | CEO (Global) | `{username, password}` | No tenant header needed |
| `POST /api/v1/auth/login` | Tenant User | `{username, password}` + `X-Tenant-ID` header | Requires tenant schema |

### 8d. JWT Claims per Login Type

**Root user token:**
```json
{
  "sub": "admin",
  "userType": "ROOT",
  "userId": 1,
  "role": "SUPER_ADMIN",
  "iat": ..., "exp": ...
}
```

**CEO token:**
```json
{
  "sub": "jane_smith",
  "userType": "CEO",
  "userId": 5,
  "role": "CEO",
  "tenantSchema": "acm_ser_91b2d0",
  "iat": ..., "exp": ...
}
```

**Tenant user token:**
```json
{
  "sub": "john_doe",
  "userType": "TENANT",
  "userId": 12,
  "roleId": 2,
  "roleName": "BRANCH_MANAGER",
  "tenantSchema": "acm_ser_91b2d0",
  "iat": ..., "exp": ...
}
```

### 8e. Files to Create

```
config/
  JwtProperties.java              ← @ConfigurationProperties("app.jwt")
  JwtService.java                 ← generateToken(), validateToken(), extractClaims()
  JwtAuthFilter.java              ← OncePerRequestFilter, sets SecurityContext

modules/auth/
  dto/
    LoginRequest.java             ← {username, password}
    AuthResponse.java             ← {token, refreshToken, userType, expiresIn}
  service/
    RootAuthService.java          ← login for root users
    CeoAuthService.java           ← login for CEO users
    TenantAuthService.java        ← login for tenant users (uses TenantContext)
    AuthServiceFactory.java       ← selects correct service by userType
  controller/
    AuthController.java           ← 3 login endpoints
```

---

## 9. Filter Execution Order (Final)

```
Request
  │
  ├─ TenantResolverFilter        → reads X-Tenant-ID → TenantContext.set()
  ├─ JwtAuthFilter               → validates Bearer token → SecurityContext.set()
  └─ UsernamePasswordAuthenticationFilter (Spring default, unused but referenced)
```

---

## 10. Authorization Strategy

After login, use Spring Security's `@PreAuthorize`:

```java
// In controllers:
@PreAuthorize("hasRole('CEO') or hasAuthority('MODULE_ACTION')")

// OR via RbacPermissionEvaluator:
@PreAuthorize("hasPermission(#moduleId, 'MODULE', 'READ')")
```

`JwtAuthFilter` populates `SecurityContextHolder` with:
- `UsernamePasswordAuthenticationToken`
- `authorities` = list of `"ROLE_roleName"` + `"MODULE_ACTION"` pairs (loaded from `role_permissions` + `user_permissions`)

---

## 11. `last_login_at` — Update on Login

Both `GlobalUser.lastLoginAt` and `User.lastLoginAt` fields are already in the DB. Update them on every successful login:
```java
user.setLastLoginAt(Instant.now());
userRepository.save(user);
```
