package com.security.rbac.config;

import com.security.rbac.multitenancy.CurrentTenantIdentifierResolverImpl;
import com.security.rbac.multitenancy.MultiTenantConnectionProviderImpl;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Wires Hibernate multi-tenancy (SCHEMA strategy) programmatically.
 *
 * <h2>Why a custom EMF bean is needed</h2>
 * Boot's JpaAutoConfiguration reads provider/resolver as class-name strings,
 * so Hibernate would instantiate them itself without Spring DI — their own
 * dependencies (e.g. DataSource) would never be injected. Supplying already-
 * constructed Spring beans here gives full DI support.
 *
 * <h2>Flyway ordering — inject, don't @DependsOn</h2>
 * Using {@code @DependsOn("flyway")} fails when a custom EMF bean is present
 * because Boot's FlywayAutoConfiguration is ordered AFTER
 * HibernateJpaAutoConfiguration,
 * which backs off when it sees our custom EMF. The bean named "flyway" is then
 * never registered in time.
 * Fix: inject {@link Flyway} as a method parameter — Spring resolves it first
 * (auto-configures it via FlywayAutoConfiguration since we have
 * spring.flyway.enabled=true), establishing the correct creation order without
 * relying on a string bean name.
 *
 * <h2>TransactionManager</h2>
 * Since JpaAutoConfiguration backs off, we must also declare the
 * {@link JpaTransactionManager} bean manually.
 */
@Configuration
public class HibernateMultiTenancyConfig {

    private final DataSource dataSource;

    @Autowired
    public HibernateMultiTenancyConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Bean
    @Primary
    public MultiTenantConnectionProvider multiTenantConnectionProvider() {
        return new MultiTenantConnectionProviderImpl(dataSource);
    }

    @Bean
    public CurrentTenantIdentifierResolver currentTenantIdentifierResolver() {
        return new CurrentTenantIdentifierResolverImpl();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            Flyway flyway,
            CurrentTenantIdentifierResolver currentTenantIdentifierResolver,
            MultiTenantConnectionProvider multiTenantConnectionProvider) {

        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan("com.security.rbac");
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Map<String, Object> jpaProperties = new HashMap<>();
        jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        jpaProperties.put("hibernate.hbm2ddl.auto", "none");
        jpaProperties.put("hibernate.show_sql", "true");
        jpaProperties.put("hibernate.format_sql", "true");
        jpaProperties.put("hibernate.multiTenancy", "SCHEMA");
        jpaProperties.put("hibernate.tenant_identifier_resolver", currentTenantIdentifierResolver);
        jpaProperties.put("hibernate.multi_tenant_connection_provider", multiTenantConnectionProvider);
        jpaProperties.put("hibernate.connection.provider_disables_autocommit", "true");

        emf.setJpaPropertyMap(jpaProperties);
        return emf;
    }

    @Bean
    public JpaTransactionManager transactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}