package com.convivo.discovery_server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.XXssConfig;

/**
 * Configuración de seguridad HTTP del servidor Eureka.
 *
 * <p>Exige autenticación HTTP Basic para el dashboard y la API {@code /eureka/**},
 * dejando públicos solo los endpoints de salud e info de Actuator que usan los
 * healthchecks de Docker. CSRF se desactiva para {@code /eureka/**} porque los
 * clientes Eureka registran instancias vía REST sin token CSRF.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Política CSP del dashboard; {@code 'unsafe-inline'} en estilos lo requiere la UI de Eureka. */
    private static final String CONTENT_SECURITY_POLICY =
        "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:;";
    /** Deniega APIs del navegador que el dashboard no usa. */
    private static final String PERMISSIONS_POLICY = "geolocation=(), microphone=(), camera=()";

    /**
     * Construye la cadena de filtros de seguridad del servidor.
     *
     * @param http constructor de seguridad HTTP provisto por Spring Security
     * @return cadena de filtros con autenticación Basic y cabeceras de seguridad
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        try {
            http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/eureka/**"))
                .headers(headers -> {
                    headers.xssProtection(XXssConfig::disable);
                    headers.contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY));
                    headers.permissionsPolicyHeader(pp -> pp.policy(PERMISSIONS_POLICY));
                    headers.referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                })
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                    .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());

            return http.build();
        } catch (Exception e) {
            throw new IllegalStateException("Error al construir la configuración de seguridad", e);
        }
    }
}
