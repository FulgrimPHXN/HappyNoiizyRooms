package org.diceordieass.users.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";//стандартный префикс HTTP-схемы авторизации для токенов

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null ||  !authHeader.startsWith(BEARER_PREFIX)) { //Если заголовка нет или он не той схемы это не ошибка.
            // Фильтр просто пропускает запрос дальше по цепочке без попытки аутентифицировать, ничего не трогая в SecurityContextHolder
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()); // отрезает сам префикс "Bearer ", оставляя только тело токена

        try {
            String username = jwtService.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            //здесь — уже подтверждённый результат: трёхаргументный конструктор принимает principal (объект пользователя),
            // credentials (null — пароль здесь незачем, мы уже всё проверили через подпись токена) и authorities (роли из UserDetails).
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            //вот то, ради которого всё затевалось: теперь для текущего запроса(и только текущего — SecurityContextHolder
            // на потоке, не на сервере в целом) Spring Security считает пользователя аутентифицированным
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | UsernameNotFoundException _) {
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }
}
