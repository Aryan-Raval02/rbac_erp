package com.security.rbac.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Declares an explicit {@link Flyway} bean for the <b>public</b> (shared)
 * schema migrations.
 *
 * <h2>Why an explicit bean is required</h2>
 * Spring Boot's {@code FlywayAutoConfiguration} is annotated
 * {@code @AutoConfigureAfter(HibernateJpaAutoConfiguration.class)}.
 * When we declare a custom {@code LocalContainerEntityManagerFactoryBean}
 * in {@link HibernateMultiTenancyConfig}, Boot's
 * {@code HibernateJpaAutoConfiguration} backs off — and so does
 * {@code FlywayAutoConfiguration}, meaning no {@code Flyway} bean is ever
 * registered. This causes:
 * 
 * <pre>
 * "required a bean of type 'org.flywaydb.core.Flyway' that could not be found"
 * </pre>
 *
 * <p>
 * By declaring the bean here we guarantee it exists. Boot's
 * {@code FlywayAutoConfiguration} is
 * {@code @ConditionalOnMissingBean(Flyway.class)}
 * so there is NO duplicate — it will simply back off.
 *
 * <h2>Ordering</h2>
 * {@link HibernateMultiTenancyConfig#entityManagerFactory} injects this
 * {@code Flyway} bean as a parameter, which forces Spring to build this bean
 * first — guaranteeing public migrations run before Hibernate starts.
 */
@Configuration
public class PublicFlywayConfig {

    /**
     * Flyway instance configured for the {@code public} schema.
     *
     * <p>
     * Migrations are applied immediately via {@code initMethod = "migrate"}.
     * Spring calls {@code migrate()} once the bean is constructed.
     */
    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas("public")
                .defaultSchema("public")
                .locations("classpath:db/migration/public")
                .table("flyway_schema_history")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .validateOnMigrate(true)
                .outOfOrder(false)
                .load();
    }
}
