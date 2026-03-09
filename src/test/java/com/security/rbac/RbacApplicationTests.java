package com.security.rbac;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Spring Boot integration test — loads the full application context.
 *
 * <p>
 * <b>Disabled by default</b> because it requires a live PostgreSQL database.
 * To run manually:
 * 
 * <pre>
 *   1. Start PostgreSQL (localhost:5432, database: rbac_erp)
 *   2. Set env vars: POSTGRES_URL, DB_USERNAME, PASSWORD
 *   3. Run: mvn test -Dtest=RbacApplicationTests
 * </pre>
 * 
 * Remove {@code @Disabled} when you have a test database configured
 * (e.g. via Testcontainers or a dedicated test profile).
 */
@Disabled("Requires a running PostgreSQL instance — remove @Disabled when DB is available")
@SpringBootTest
class RbacApplicationTests {

	@Test
	void contextLoads() {
		// Verifies that the application context starts without errors
	}
}
