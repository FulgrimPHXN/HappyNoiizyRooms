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

        String token = jwtService.generateToken(user.getUserName());
        String refreshToken = refreshTokenService.createToken(user.getId());
        return new LoginResult(user, token, refreshToken);

        /*ошибка для исключительного случая когда пользователь испарился между проверкой и повторным чтением(практически недостижимого)
        ошибки неверного логина или пароля автоматически бросаются authenticationManager.authenticate(...),
        стоит только их прописать в хендлере для нормального кода исключение*/
    }

    //обновление пары токенов по старому refresh-токену
    @Transactional
    public LoginResult refresh(String rawOldToken) {
        //rotate сам проверяет токен на существование/отзыв/срок жизни и бросает InvalidRefreshTokenException,
        //если что-то не так — здесь этот случай отдельно не обрабатываем
        UUID userId = refreshTokenService.rotate(rawOldToken);

        //пользователь не может исчезнуть, пока жив его refresh-токен (внешних ключей на удаление пользователя пока нет),
        //но IllegalStateException — честный сигнал, что инвариант нарушен, а не тихая NPE где-то дальше
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Владелец refresh-токена не найден в БД: " + userId));

        String newAccessToken = jwtService.generateToken(user.getUserName());
        //новый refresh выпускается отдельным вызовом — rotate() больше не делает этого сам
        String newRefreshToken = refreshTokenService.createToken(userId);
        return new LoginResult(user, newAccessToken, newRefreshToken);
    }

    //логаут — это отзыв конкретного refresh-токена, а не повторный вход
    public void logout(String rawToken) {
        refreshTokenService.revoke(rawToken);
    }
}
