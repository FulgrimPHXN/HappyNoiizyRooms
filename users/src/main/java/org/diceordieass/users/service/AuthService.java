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

@Service
@RequiredArgsConstructor
public class AuthService {

    private  final UserRepository userRepository;
    private  final PasswordEncoder passwordEncoder;
    private  final AuthenticationManager authManager;
    private final JwtService jwtService;

     public static record LoginResult(User user, String token) {}

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
        return new LoginResult(user, token);

        /*ошибка для исключительного случая когда пользователь испарился между проверкой и повторным чтением(практически недостижимого)
        ошибки неверного логина или пароля автоматически бросаются authenticationManager.authenticate(...),
        стоит только их прописать в хендлере для нормального кода исключение*/
    }


}
