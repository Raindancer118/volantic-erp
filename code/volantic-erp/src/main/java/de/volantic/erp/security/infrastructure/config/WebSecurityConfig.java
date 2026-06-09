package de.volantic.erp.security.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Web security wiring of the ERP. The ERP is a pure <strong>OIDC resource server</strong>: every
 * request carries a JWT signed by Authentik (bundled per installation); the {@code sub} claim is the
 * OIDC subject the {@link de.volantic.erp.security.AuthorizationService} decides on.
 *
 * <p>Deliberate: actuator health/info (including k8s liveness/readiness probes) are reachable
 * unauthenticated — otherwise the probes would receive 401. Everything else requires a valid token.
 * Stateless (no session cookie) ⇒ CSRF disabled. Method security ({@link EnableMethodSecurity}) is on
 * so that enforcement happens at the service boundary via {@code @PreAuthorize} (ADR-0004).
 */
@Configuration
@EnableMethodSecurity
class WebSecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(opt -> {})
                        .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31_536_000)));
        return http.build();
    }

    /**
     * Plugs the project's own {@link PermissionEvaluator} into the SpEL evaluation of
     * {@code @PreAuthorize("hasPermission(...)")}, so every method authorization runs through the
     * central {@link de.volantic.erp.security.AuthorizationService}.
     */
    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(PermissionEvaluator permissionEvaluator) {
        var handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(permissionEvaluator);
        return handler;
    }
}
