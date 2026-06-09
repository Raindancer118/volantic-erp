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
 * Web-Security-Verdrahtung des ERP. Das ERP ist reiner <strong>OIDC-Resource-Server</strong>: jeder
 * Request trägt ein von Authentik ({@code auth.volantic.de}) signiertes JWT; der {@code sub}-Claim ist
 * das OIDC-Subject, über das der {@link de.volantic.erp.security.AuthorizationService} entscheidet.
 *
 * <p>Bewusst: Actuator-Health/-Info (inkl. k8s-Liveness/Readiness-Probes) sind unauthentifiziert
 * erreichbar — sonst würden die Probes 401 erhalten. Alles andere erfordert ein gültiges Token.
 * Stateless (kein Session-Cookie) ⇒ CSRF deaktiviert. Methoden-Security ({@link EnableMethodSecurity})
 * ist aktiv, damit Enforcement an der Service-Grenze via {@code @PreAuthorize} greift (ADR-0004).
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
     * Hängt den projekteigenen {@link PermissionEvaluator} in die SpEL-Auswertung von
     * {@code @PreAuthorize("hasPermission(...)")} ein, sodass jede Methoden-Autorisierung über den
     * zentralen {@link de.volantic.erp.security.AuthorizationService} läuft.
     */
    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(PermissionEvaluator permissionEvaluator) {
        var handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(permissionEvaluator);
        return handler;
    }
}
