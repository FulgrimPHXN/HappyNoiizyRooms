package org.diceordieass.users.config;

import lombok.RequiredArgsConstructor;
import org.diceordieass.users.security.JwtAuthenticationFilter;
import org.diceordieass.users.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        //конкретная реализация AuthenticationProvider, умеющая проверять логин/пароль через UserDetailsService
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        //говорит, каким алгоритмом сверять пароль с хэшем в БД
        authProvider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(authProvider);
    }


    @Bean
    //Цепочка вызовов на http — каждый метод настраивает свою, независимую друг от друга заботу
    // (порядок между ними, в отличие от матчеров внутри authorizeHttpRequests, не важен)
    @SuppressWarnings("java:S1149")
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, RestAuthenticationEntryPoint restEntryPoint) {
        //Это весь путь запроса целиком: JwtAuthenticationFilter пытается аутентифицировать по токену → authorizeHttpRequests решает,
        // пускать или нет, с учётом того, что permitAll()-пути вообще не требуют результата первого шага → если отклонено,
        // exceptionHandling передаёт решение твоему RestAuthenticationEntryPoint, который формирует честный 401.
        return http
                .csrf(AbstractHttpConfigurer::disable)
                //запрещает создавать или читать HttpSession вообще
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                //говорит Spring Security: если запрос не прошёл аутентификацию на защищённом пути, не отдавай свой дефолтный голый 403,
                // а вызови restEntryPoint.commence(...), который вернёт честный 401 в формате ProblemDetail.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(restEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        //refresh и logout проверяют право действовать сами, по raw refresh-токену в теле запроса —
                        //это их credential, а не access-JWT в заголовке (которого на момент вызова refresh уже может не быть валидным)
                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                        //метрики/health открыты без JWT ради удобства локальной разработки (localhost не торчит
                        //наружу) — на реальном проде это обычно прячут за отдельным management-портом/приватной сетью
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated())
                //Вставляет твой JwtAuthenticationFilter в цепочку перед стандартным (из коробки Spring Security) UsernamePasswordAuthenticationFilter.
                // Именно этот фильтр читает заголовок Authorization, проверяет токен через JwtService и кладёт Authentication в SecurityContextHolder,
                // если токен валиден.
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}