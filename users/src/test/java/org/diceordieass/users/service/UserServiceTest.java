package org.diceordieass.users.service;


import org.diceordieass.users.dto.ChangeUserNameRequest;
import org.diceordieass.users.dto.ChangeUserPasswordRequest;
import org.diceordieass.users.entity.User;
import org.diceordieass.users.exception.IncorrectPasswordException;
import org.diceordieass.users.exception.UserAlreadyExistsException;
import org.diceordieass.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private Authentication authentication;

    private UserService userService;

    @BeforeEach
     void setUp() {
        userService = new UserService(userRepository, passwordEncoder, refreshTokenService);
    }

    @Test
    void changePassword_correctOldPassword_changesPasswordAndRevokesAllTokens() {
        User user = new User("someUser", "oldHash");
        ChangeUserPasswordRequest request = new ChangeUserPasswordRequest("oldPass", "newPass");

        when(authentication.getName()).thenReturn(user.getUserName());
        when(userRepository.findByUserName(user.getUserName())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode(request.newPassword())).thenReturn("NewHash");

        userService.changePassword(authentication, request);

        assertThat(user.getPasswordHash()).isEqualTo("NewHash");
        verify(refreshTokenService).revokeAllForUser(user.getId());
    }

    @Test
    void changePassword_incorrectOldPassword_throwsIncorrectPasswordExceptionAndDoesNothing() {
        User user = new User("someUser", "oldHash");
        ChangeUserPasswordRequest request = new ChangeUserPasswordRequest("incorrectOldPassword", "newPass");

        when(authentication.getName()).thenReturn(user.getUserName());
        when(userRepository.findByUserName(user.getUserName())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())).thenReturn(false);

        assertThatExceptionOfType(IncorrectPasswordException.class)
                .isThrownBy(() -> userService.changePassword(authentication, request));

        assertThat(user.getPasswordHash()).isEqualTo("oldHash");
        verify(refreshTokenService, never()).revokeAllForUser(user.getId());
    }

    @Test
    void changeUserName_nameAvailable_renameUser() {
        User user = new User("someUser", "someHash");
        ChangeUserNameRequest request = new ChangeUserNameRequest("newUserName");

        when(userRepository.findByUserName(user.getUserName())).thenReturn(Optional.of(user));
        when(userRepository.findByUserName(request.userName())).thenReturn(Optional.empty());
        when(authentication.getName()).thenReturn(user.getUserName());

        User changedUser = userService.changeUserName(authentication, request);

        assertThat(changedUser.getUserName()).isEqualTo("newUserName");
    }

    @Test
    void changeUserName_nameTaken_throwsUserAlreadyExistsAndDoesNothing() {
        User user = new User("someUser", "someHash");
        User otherUser = new User("someUser1", "someOtherHash");

        ChangeUserNameRequest request = new ChangeUserNameRequest("newUserName");

        when(authentication.getName()).thenReturn(user.getUserName());
        when(userRepository.findByUserName(user.getUserName())).thenReturn(Optional.of(user));
        when(userRepository.findByUserName(request.userName())).thenReturn(Optional.of(otherUser));

        assertThatExceptionOfType(UserAlreadyExistsException.class)
                .isThrownBy(() -> userService.changeUserName(authentication, request));
        assertThat(user.getUserName()).isEqualTo("someUser");
    }
}
