package org.diceordieass.rooms.config;

import lombok.RequiredArgsConstructor;
import org.diceordieass.rooms.security.JwtAuthenticationFilter;
import org.diceordieass.rooms.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/*
 * В отличие от users.config.SecurityConfig, здесь нет бинов AuthenticationManager/
 * PasswordEncoder — :rooms не делает login и не сверяет пароли, он только проверяет
 * чужую подпись на уже готовом токене. Поэтому весь bean один — сама цепочка фильтров.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, RestAuthenticationEntryPoint restEntryPoint) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(restEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
