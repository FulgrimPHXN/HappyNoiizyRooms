package org.diceordieass.users.config;

import org.diceordieass.users.security.RestAccessDeniedHandler;
import org.diceordieass.users.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
 * :users больше не выпускает токены и не хранит credentials — Keycloak единственный
 * источник identity (см. CLAUDE.md, раздел 4). Поэтому здесь не осталось бинов
 * AuthenticationManager/PasswordEncoder — этот сервис теперь чистый OAuth2 Resource
 * Server: Spring Security сам тянет JWKS Keycloak по issuer-uri
 * (application.properties) и проверяет подпись/срок жизни токена.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        //Keycloak кладёт realm-роли в claim realm_access.roles, а не в "scope"/"scp",
        //который JwtAuthenticationConverter читает по умолчанию — без этой настройки
        //hasRole("ADMIN") ниже никогда бы не находил роль ADMIN
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess == null) {
                return List.of();
            }
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.getOrDefault("roles", List.of());
            return roles.stream()
                    .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
        });
        return converter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RestAuthenticationEntryPoint restEntryPoint,
            RestAccessDeniedHandler restAccessDeniedHandler,
            JwtAuthenticationConverter jwtAuthenticationConverter) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        //health открыт без JWT — его читают внешние health-check'и, не люди
                        .requestMatchers("/actuator/health").permitAll()
                        //метрики видят внутренние цифры сервиса — доступ только роли ADMIN
                        //из realm_access.roles токена (CLAUDE.md, раздел 4)
                        .requestMatchers("/actuator/metrics/**", "/actuator/prometheus").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler(restAccessDeniedHandler))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(restEntryPoint)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }
}