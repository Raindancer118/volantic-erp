package de.volantic.erp.security.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

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
     * JWT decoder with issuer <em>and</em> audience validation. The default resource-server setup only
     * validates the signature and issuer — so any token minted by the bundled Authentik for a
     * <em>different</em> application on the same instance would also be accepted here (a confused-deputy
     * risk). When {@code OIDC_AUDIENCE} is configured, we additionally require it in the token's
     * {@code aud} claim, so a token must have been issued specifically for this ERP. The audience is
     * optional (empty by default) so single-application installs are not forced to set it.
     */
    @Bean
    JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
            @Value("${OIDC_AUDIENCE:}") String audience) {
        // Lazy: the JWKS is fetched on the first request, not at startup, so the app still boots when the
        // IdP is briefly unreachable (matches the existing application.yml rationale).
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(JwtValidators.createDefaultWithIssuer(issuerUri));
        if (StringUtils.hasText(audience)) {
            validators.add(new JwtClaimValidator<List<String>>(
                    JwtClaimNames.AUD, aud -> aud != null && aud.contains(audience)));
        }
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
        return decoder;
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
