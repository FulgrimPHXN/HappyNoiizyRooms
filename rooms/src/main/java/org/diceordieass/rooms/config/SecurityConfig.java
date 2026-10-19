package org.diceordieass.rooms.config;

import lombok.RequiredArgsConstructor;
import org.diceordieass.rooms.security.JwtAuthConverter;
import org.diceordieass.rooms.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/*
 * :rooms больше не проверяет подпись сам (jjwt + общий HMAC-секрет) — он чистый
 * OAuth2 Resource Server: Spring Security сам тянет JWKS Keycloak по issuer-uri
 * (application.properties) и проверяет подпись/срок жизни токена. jwtAuthConverter
 * сохраняет старый контракт (principal — голый UUID, не Jwt-объект), поэтому
 * контроллеры не меняются.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthConverter jwtAuthConverter;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, RestAuthenticationEntryPoint restEntryPoint) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(restEntryPoint)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter)))
                .build();
    }
}
