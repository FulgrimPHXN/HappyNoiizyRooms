package org.diceordieass.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/*
 * Единственное место в Gateway, где вообще нужен код из Spring Security — не
 * SecurityFilterChain, а голый декодер подписи. NimbusJwtDecoder сам тянет и
 * кэширует JWKS Keycloak по jwk-set-uri и сверяет RS256-подпись каждого токена;
 * подключать ради этого spring-boot-starter-security не нужно (JwtGlobalFilter
 * остаётся единственным, кто решает "пропустить/отбить", без SecurityContextHolder).
 */
@Configuration
public class JwtDecoderConfig {

    @Bean
    public JwtDecoder jwtDecoder(@Value("${app.jwt.jwk-set-uri}") String jwkSetUri) {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}
