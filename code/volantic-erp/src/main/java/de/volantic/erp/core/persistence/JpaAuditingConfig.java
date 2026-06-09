package de.volantic.erp.core.persistence;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Activates Spring Data JPA auditing for the GoBD tracking columns on {@code AbstractEntity}.
 *
 * <p>The timestamp provider returns {@link OffsetDateTime} so {@code @CreatedDate}/{@code @LastModifiedDate}
 * line up with the {@code timestamptz} columns. The auditor is the acting OIDC subject taken from the
 * security context (empty for unauthenticated/system flows such as migrations or tests, leaving
 * {@code created_by/modified_by} null).
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider", auditorAwareRef = "auditorAware")
class JpaAuditingConfig {

    @Bean
    DateTimeProvider auditingDateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now());
    }

    @Bean
    AuditorAware<String> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }
            return Optional.ofNullable(authentication.getName());
        };
    }
}
