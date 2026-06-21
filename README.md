# Multi-Tenant Enterprise Resource Planning (ERP) Backend

An enterprise-grade, high-performance **SaaS ERP Backend** built with Java 21 and Spring Boot. This application is designed to securely manage business operations for multiple independent organizations simultaneously, utilizing strict data isolation and a dynamic security layer optimized for high concurrency.

## 🚀 Key Features

- **True SaaS Multi-Tenancy:** Implements a "Shared-Database, Isolated-Schema" architecture using PostgreSQL. Each registered organization (Tenant) automatically receives its own isolated schema. Database connections are dynamically routed (`SET search_path`), ensuring 100% data isolation.
- **Dynamic Hybrid RBAC (Role-Based Access Control):** Moves beyond hardcoded roles to a dynamic permission engine. Cross-verifies global roles (`ROOT`, `CEO`) while enforcing granular, localized permissions (e.g., `BRANCH_MANAGEMENT_READ`) evaluated dynamically at runtime via `@PreAuthorize`.
- **Stateless JWT Authentication:** Implements a highly optimized "Sparse Token" architecture. Tokens remain lightweight to avoid header bloat, while exhaustive permission arrays are eagerly cached into the server context via `AuthorityLoaderService` upon request validation.
- **High Concurrency & Performance:** Natively leverages **Java 21 Virtual Threads** and optimized HikariCP connection pooling to achieve high throughput and scalability with a minimal memory footprint.
- **Automated Schema Management:** Integrates **Flyway** for robust, automated database migrations, ensuring synchronized schema evolution across 100+ isolated tenant environments safely.

## 🛠️ Technology Stack

- **Core:** Java 21, Spring Boot 4.x (WebMVC, Data JPA, Security)
- **Database & Migration:** PostgreSQL, Flyway, HikariCP, Hibernate
- **Security:** Spring Security, JSON Web Tokens (jjwt 0.12.6)
- **Caching & Mapping:** Caffeine Cache, MapStruct, Lombok
- **API Documentation:** Springdoc OpenAPI / Swagger UI

## 📂 Architecture Documentation

Deep-dive documentation is included within the repository. Please refer to the following files to understand the system design:

- [rbac.md](./rbac.md) - Explains the Dynamic, Tenant-Aware Hybrid RBAC architecture and JWT lifecycle.
- [system-capacity.md](./system-capacity.md) - Details the performance constraints, bottleneck analysis, and scaling path.
- [login.md](./login.md) & [signup.md](./signup.md) - Outlines the authentication workflows.

## ⚙️ Getting Started

### Prerequisites
- JDK 21
- Maven 3.8+
- PostgreSQL 15+ (or Docker)

### Environment Setup
1. Clone the repository.
2. Create a `.env` file or set the following environment variables (refer to `.env.example`):
   ```env
   POSTGRES_URL=jdbc:postgresql://localhost:5432/your_db
   DB_USERNAME=your_db_user
   PASSWORD=your_db_password
   JWT_SECRET=your_super_secret_key
   JWT_EXPIRATION_MS=86400000
   ROOT_EMAIL=admin@platform.com
   ROOT_PASSWORD=admin_password
   ROOT_NAME=Super Admin
   ROOT_USERNAME=admin
   ```
3. *(Optional)* Use the included `docker-compose.yaml` to spin up a local PostgreSQL instance.

### Build and Run
Build the application utilizing the Maven wrapper:
```bash
./mvnw clean install
```
Start the application:
```bash
./mvnw spring-boot:run
```

The server will start on `http://localhost:8080`.

## 📖 API Documentation
Once the server is running, you can access the interactive API documentation and test endpoints via Swagger UI at:
`http://localhost:8080/swagger-ui/index.html` (or the respective context path if configured).
