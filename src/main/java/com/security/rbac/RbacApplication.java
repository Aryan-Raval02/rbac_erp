package com.security.rbac;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Application entry point.
 *
 * <p>
 * {@code @ConfigurationPropertiesScan} automatically registers all
 * {@code @ConfigurationProperties} beans (e.g. {@code RootUserProperties})
 * found in this package and sub-packages.
 */
@SpringBootApplication
@ConfigurationPropertiesScan("com.security.rbac")
public class RbacApplication {

	public static void main(String[] args) {
		SpringApplication.run(RbacApplication.class, args);
	}
}
