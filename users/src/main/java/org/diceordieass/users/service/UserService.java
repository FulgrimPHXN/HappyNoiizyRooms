package org.diceordieass.users.service;

import lombok.RequiredArgsConstructor;
import org.diceordieass.users.entity.User;
import org.diceordieass.users.dto.UserDto;
import org.diceordieass.users.exception.UserAlreadyExistsException;
import org.diceordieass.users.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private  final UserRepository userRepository;
    private  final PasswordEncoder passwordEncoder;

    public User createUser(UserDto userDto) {
        userRepository.findByUserName(userDto.userName()).ifPresent(existing -> {
            throw new UserAlreadyExistsException(userDto.userName());
        });
        return userRepository.save(new User(userDto.userName(), passwordEncoder.encode(userDto.rawPassword())));
    }
}
