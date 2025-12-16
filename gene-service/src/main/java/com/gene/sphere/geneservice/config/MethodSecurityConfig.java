package com.gene.sphere.geneservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;

/**
 * Enables method-level security using {@code @PreAuthorize} and {@code @PostAuthorize} annotations.
 *
 * <p>This allows securing individual controller methods with role-based access control:</p>
 * <pre>{@code
 * @PreAuthorize("hasRole('ADMIN')")
 * public ResponseEntity<ClearResult> clearCache() { ... }
 * }</pre>
 *
 * <p><strong>Key Points:</strong></p>
 * <ul>
 *   <li>{@code hasRole('ADMIN')} checks for "ROLE_ADMIN" authority (auto-prefixes "ROLE_")</li>
 *   <li>{@code hasAuthority('ADMIN')} checks for exact "ADMIN" authority (no prefix)</li>
 *   <li>Extending {@link GlobalMethodSecurityConfiguration} allows custom security logic if needed</li>
 * </ul>
 */
@Configuration
@EnableGlobalMethodSecurity(
        // Enables @PreAuthorize and @PostAuthorize annotations
        // Example: @PreAuthorize("hasRole('ADMIN')")
        prePostEnabled = true
)
public class MethodSecurityConfig extends GlobalMethodSecurityConfiguration {
}