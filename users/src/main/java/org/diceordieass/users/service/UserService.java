package org.diceordieass.users.service;


import lombok.RequiredArgsConstructor;
import org.diceordieass.users.dto.ChangeUserNameRequest;
import org.diceordieass.users.dto.ChangeUserPasswordRequest;
import org.diceordieass.users.entity.User;
import org.diceordieass.users.exception.IncorrectPasswordException;
import org.diceordieass.users.exception.UserAlreadyExistsException;
import org.diceordieass.users.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void changePassword(Authentication authentication , ChangeUserPasswordRequest request) {
        User user = userRepository.findByUserName(authentication.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Пользователь " + authentication.getName() + " не найден"));
        Optional.of(user)
                .filter(u -> passwordEncoder.matches(request.oldPassword(), u.getPasswordHash()))
                .ifPresentOrElse(
                        u -> u.changePassword(passwordEncoder.encode(request.newPassword())),
                        () -> {throw new IncorrectPasswordException("Пароли не совпадают");}
                );
    }

    @Transactional
    public User changeUserName(Authentication authentication , ChangeUserNameRequest request) {
        User user = userRepository.findByUserName(authentication.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Пользователь " + authentication.getName() + " не найден"));
        assertUserNameAvailable(request);
        user.rename(request.userName());
        return user;
    }

    private void assertUserNameAvailable(ChangeUserNameRequest request) {
        userRepository.findByUserName(request.userName()).ifPresent(existingUser -> {
            throw new UserAlreadyExistsException(request.userName());});
    }
}
