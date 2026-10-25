package org.diceordieass.users.service;

import org.diceordieass.users.dto.UserDto;
import org.diceordieass.users.entity.User;
import org.diceordieass.users.exception.InvalidRefreshTokenException;
import org.diceordieass.users.exception.UserAlreadyExistsException;
import org.diceordieass.users.repository.UserRepository;
import org.diceordieass.users.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private  RefreshTokenService refreshTokenService;
    @Mock
    private  JwtService jwtService;

    @Mock
    private Authentication authentication;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, refreshTokenService, jwtService);
    }

    @Test
    void createUser_newUserName_savesEncodedUserAndReturnsIt() {
        UserDto userDto = new UserDto("NewUserName", "password");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        when(userRepository.findByUserName(userDto.userName())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(userDto.rawPassword())).thenReturn("hashedPassword");

        authService.createUser(userDto);

        verify(userRepository).save(captor.capture());

        assertThat(captor.getValue().getUserName()).isEqualTo("NewUserName");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashedPassword");
    }

    @Test
    void createUser_usernameTaken_throwsUserAlreadyExistsException() {
        UserDto newUser = new UserDto("ExistingName", "password");
        User existingUser = new User("ExistingName", "somePassword");

        when(userRepository.findByUserName(existingUser.getUserName())).thenReturn(Optional.of(existingUser));

        assertThatExceptionOfType(UserAlreadyExistsException.class)
                .isThrownBy(() -> authService.createUser(newUser));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_validCredentials_returnTokenForAuthenticatedUser() {
        UserDto userDto = new UserDto("NewUserName", "password");
        User existingUser = new User("NewUserName", "somePassword");

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getName()).thenReturn(userDto.userName());
        when(userRepository.findByUserName(userDto.userName())).thenReturn(Optional.of(existingUser));
        when(jwtService.generateToken(existingUser.getId())).thenReturn("token");
        when(refreshTokenService.createToken(existingUser.getId())).thenReturn("rToken");

        AuthService.LoginResult result = authService.login(userDto);

        assertThat(result.user()).isEqualTo(existingUser);
        assertThat(result.token()).isEqualTo("token");
        assertThat(result.refreshToken()).isEqualTo("rToken");
    }

    @Test
    void refresh_validOldToken_rotatesAndReturnsNewTokenPair() {
        User user = new User("someUser", "someHash");

        when(refreshTokenService.rotate("oldToken")).thenReturn(user.getId());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user.getId())).thenReturn("newAccessToken");
        when(refreshTokenService.createToken(user.getId())).thenReturn("newRefreshToken");

        AuthService.LoginResult result = authService.refresh("oldToken");

        assertThat(result.user()).isEqualTo(user);
        assertThat(result.token()).isEqualTo("newAccessToken");
        assertThat(result.refreshToken()).isEqualTo("newRefreshToken");
    }

    @Test
    void refresh_invalidOldToken_propagatesExceptionWithoutTouchingUser() {
        when(refreshTokenService.rotate("badToken"))
                .thenThrow(new InvalidRefreshTokenException("Invalid Refresh Token"));

        assertThatExceptionOfType(InvalidRefreshTokenException.class)
                .isThrownBy(() -> authService.refresh("badToken"));

        verify(userRepository, never()).findById(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void logout_delegatesToRefreshTokenServiceRevoke() {
        authService.logout("someToken");

        verify(refreshTokenService).revoke("someToken");
    }

}
