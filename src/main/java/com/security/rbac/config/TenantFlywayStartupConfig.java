package com.security.rbac.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class TenantFlywayStartupConfig {

    @Bean
    public ApplicationRunner migrateTenantsOnStartup(DataSource dataSource) {
        return args -> {
            List<String> tenantSchemas = loadTenantSchemas(dataSource);

            for (String schema : tenantSchemas) {
                String normalizedSchema = schema.toLowerCase().trim();

                Flyway.configure()
                        .dataSource(dataSource)
                        .schemas(normalizedSchema)
                        .defaultSchema(normalizedSchema)
                        .locations("classpath:db/migration/tenant")
                        .table("flyway_schema_history")
                        .baselineOnMigrate(true)
                        .baselineVersion("0")
                        .validateOnMigrate(true)
                        .load()
                        .migrate();
            }
        };
    }

    private List<String> loadTenantSchemas(DataSource dataSource) throws Exception {
        List<String> schemas = new ArrayList<>();

        String sql = "select schema_name from public.tenant_registry where status = ?";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, "ACTIVE");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    schemas.add(rs.getString("schema_name"));
                }
            }
        }

        return schemas;
    }
}