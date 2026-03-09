# Flyway Schema Capitalisation Bug — Analysis & Fix

## The Problem

When a company signs up, the tenant schema name is derived from the company name and passed through the system. **In certain scenarios, Flyway creates a brand-new schema instead of migrating the existing one** — leading to duplicate schemas where one is lowercase and one has capital letters.

---

## Root Causes (3 separate issues found)

### 🔴 Issue 1 — `createSchemaIfNotExists` Uses Double-Quoted Identifier

**File:** `TenantMigrationService.java` — line 103

```java
// CURRENT (BUG)
String sql = "CREATE SCHEMA IF NOT EXISTS \"" + schema + "\"";
```

**Why this breaks:**

PostgreSQL treats quoted identifiers as **case-sensitive**. So:

| SQL | Schema created | Case-sensitive? |
|-----|---------------|-----------------|
| `CREATE SCHEMA IF NOT EXISTS acme_corp` | `acme_corp` | no (safe) |
| `CREATE SCHEMA IF NOT EXISTS "acme_corp"` | `"acme_corp"` | **yes** |
| `CREATE SCHEMA IF NOT EXISTS "Acme_Corp"` | `"Acme_Corp"` ← **NEW SCHEMA** | **yes** |

If `schema` ever contains an uppercase letter (even one), a new quoted schema is created even though `acme_corp` (unquoted, lowercase) already exists.

**Fix:** Remove the quotes — PostgreSQL automatically lowercases unquoted identifiers:

```java
// FIXED
String sql = "CREATE SCHEMA IF NOT EXISTS " + schema;
// schema is already guaranteed lowercase by sanitize()
```

---

### 🔴 Issue 2 — `sanitizeSchemaName` in `MultiTenantConnectionProviderImpl` Accepts Uppercase

**File:** `MultiTenantConnectionProviderImpl.java` — lines 94–99

```java
// CURRENT (BUG)
if (schema == null || !schema.matches("[a-zA-Z0-9_]+")) {  // ← accepts A-Z
    ...
}
return schema.toLowerCase(); // toLowerCase called AFTER validation
```

The `[a-zA-Z0-9_]+` regex allows uppercase letters through validation. Although `.toLowerCase()` is called at the end, if any code path uses the value **before** this return (e.g., logging, caching, lock keys), the mixed-case value leaks.

**Fix:** Validate **after** lowercasing:

```java
// FIXED
private String sanitizeSchemaName(String schema) {
    if (schema == null || schema.isBlank()) {
        throw new IllegalArgumentException("Schema name must not be blank.");
    }
    String lower = schema.toLowerCase().trim();
    if (!lower.matches("[a-z][a-z0-9_]{0,62}")) {
        throw new IllegalArgumentException(
            "Invalid schema name '" + schema + "'. Must be lowercase alphanumeric + underscores.");
    }
    return lower;
}
```

---

### 🟡 Issue 3 — Flyway `.schemas(schema)` Sends Raw Value to Driver

**File:** `TenantMigrationService.java` — line 137

```java
Flyway flyway = Flyway.configure()
    .schemas(schema)          // ← sent as-is to the JDBC driver
    .defaultSchema(schema)    // ← same
    ...
```

Flyway passes the schema name to the driver. If `schema` contains uppercase, the JDBC driver may quote it before sending to PostgreSQL — resulting in case-sensitive schema creation.

**Fix:** Explicitly lowercase before passing to Flyway (even if `sanitize()` already does this, be explicit):

```java
String safeSchema = sanitize(tenantSchema); // already lowercase
Flyway flyway = Flyway.configure()
    .schemas(safeSchema.toLowerCase())
    .defaultSchema(safeSchema.toLowerCase())
    ...
```

---

## Summary of Fixes

### Fix 1 — `TenantMigrationService.java`

```diff
- String sql = "CREATE SCHEMA IF NOT EXISTS \"" + schema + "\"";
+ String sql = "CREATE SCHEMA IF NOT EXISTS " + schema;
```

### Fix 2 — `MultiTenantConnectionProviderImpl.java`

```diff
  private String sanitizeSchemaName(String schema) {
-     if (schema == null || !schema.matches("[a-zA-Z0-9_]+")) {
+     if (schema == null || schema.isBlank()) {
          throw new IllegalArgumentException(
-                 "Invalid schema name '" + schema + "'...");
+                 "Schema name must not be blank.");
      }
-     return schema.toLowerCase();
+     String lower = schema.toLowerCase().trim();
+     if (!lower.matches("[a-z][a-z0-9_]{0,62}")) {
+         throw new IllegalArgumentException(
+                 "Invalid schema name '" + schema + "'.");
+     }
+     return lower;
  }
```

---

## Why "Fetching from DB Works But Migration Creates New"

When you **fetch** tenants from `public.tenant_registry`, the `schema_name` column already stores the correct lowercase value (e.g., `acme_corp`). That value is passed to `SET search_path TO "acme_corp"` — which works fine.

But during **sign-up**, if the company name slips through with any uppercase:

```
"Acme Corp" → deriveSchemaName() → "acme_corp"  ✅ correct
```

However if the value is constructed elsewhere (e.g., from an HTTP header `X-Tenant-ID: AcmeCorp`), it bypasses `deriveSchemaName()` and goes directly to `TenantMigrationService`. With the quoted `CREATE SCHEMA IF NOT EXISTS "AcmeCorp"`, PostgreSQL sees it as a new schema.

---

## Defence-in-Depth Checklist

| Layer | Current | Should Be |
|-------|---------|-----------|
| `SignUpServiceImpl.deriveSchemaName()` | ✅ lowercases | ✅ keep |
| `TenantMigrationService.sanitize()` | ✅ lowercases + validates | ✅ keep |
| `createSchemaIfNotExists()` SQL | ❌ double-quoted | ✅ unquoted |
| `MultiTenantConnectionProviderImpl.sanitizeSchemaName()` | ❌ validates before lowercase | ✅ lowercase first |
| Flyway `.schemas()` call | 🟡 relies on caller | ✅ add explicit `.toLowerCase()` |
| `TenantResolverFilter` (HTTP header) | 🟡 no lowercase guard | ✅ add `.toLowerCase()` on header read |

---

## Optional — Add Guard in `TenantResolverFilter`

When reading `X-Tenant-ID` from the HTTP header, always lowercase it:

```java
// In TenantResolverFilter (wherever header is read)
String tenantId = request.getHeader("X-Tenant-ID");
if (tenantId != null) {
    TenantContext.setCurrentTenant(tenantId.toLowerCase().trim());
}
```

This ensures even if a client sends `X-Tenant-ID: AcmeCorp`, it is normalised to `acme_corp` before Hibernate uses it.
