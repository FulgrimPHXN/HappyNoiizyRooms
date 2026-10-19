package org.diceordieass.users.security;

import lombok.RequiredArgsConstructor;
import org.diceordieass.users.repository.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Загружает пользователя из {@link UserRepository} и конвертирует его
 * в {@link UserDetails} для Spring Security, выставляя роль пользователя
 * ({@link UserRole}) в качестве единственного authority.
 * <p>
 * Два разных способа найти пользователя нужны двум разным вызывающим: логин (через
 * {@code AuthenticationManager}) знает только введённый пользователем username, а
 * {@code JwtAuthenticationFilter} — только userId из {@code sub} токена.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    //реализация интерфейса UserDetailsService — её вызывает AuthenticationManager во время /login
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUserName(username)
                .map(this::toUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь " + username + " не найден"));
    }

    //не часть UserDetailsService — вызывается напрямую из JwtAuthenticationFilter,
    //у которого на руках userId из токена, а не username
    public UserDetails loadUserById(UUID id) throws UsernameNotFoundException {
        return userRepository.findById(id)
                .map(this::toUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь " + id + " не найден"));
    }

    private UserDetails toUserDetails(org.diceordieass.users.entity.User user) {
        return User.withUsername(user.getUserName())
                .password(user.getPasswordHash())
                .authorities(user.getRole().name())
                .build();
    }
}
