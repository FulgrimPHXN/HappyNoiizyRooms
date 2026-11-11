package org.diceordieass.users.service;

import lombok.RequiredArgsConstructor;
import org.diceordieass.users.entity.User;
import org.diceordieass.users.dto.UserDto;
import org.diceordieass.users.exception.UserAlreadyExistsException;
import org.diceordieass.users.repository.UserRepository;
import org.diceordieass.users.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private  final UserRepository userRepository;
    private  final PasswordEncoder passwordEncoder;
    private  final AuthenticationManager authManager;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

     public static record LoginResult(User user, String token, String refreshToken) {}

    public User createUser(UserDto userDto) {
        userRepository.findByUserName(userDto.userName()).ifPresent(existing -> {
            throw new UserAlreadyExistsException(userDto.userName());
        });
        return userRepository.save(new User(userDto.userName(), passwordEncoder.encode(userDto.rawPassword())));
    }

    public LoginResult login(UserDto userDto) {
        Authentication authentication = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(userDto.userName(), userDto.rawPassword()));

        User user = userRepository.findByUserName(authentication.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Аутентифицированный пользователь не найден в БД: " + authentication.getName()));

        String token = jwtService.generateToken(user.getId());
        String refreshToken = refreshTokenService.createToken(user.getId());
        return new LoginResult(user, token, refreshToken);

    }

    @Transactional
    public LoginResult refresh(String rawOldToken) {

        UUID userId = refreshTokenService.rotate(rawOldToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Владелец refresh-токена не найден в БД: " + userId));

        String newAccessToken = jwtService.generateToken(user.getId());

        String newRefreshToken = refreshTokenService.createToken(userId);
        return new LoginResult(user, newAccessToken, newRefreshToken);
    }

    public void logout(String rawToken) {
        refreshTokenService.revoke(rawToken);
    }
}
