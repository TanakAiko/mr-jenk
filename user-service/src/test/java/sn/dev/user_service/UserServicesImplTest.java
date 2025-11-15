package sn.dev.user_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import sn.dev.user_service.data.entities.User;
import sn.dev.user_service.data.repositories.UserRepositories;
import sn.dev.user_service.services.JWTServices;
import sn.dev.user_service.services.impl.UserServicesImpl;

@ExtendWith(MockitoExtension.class)
public class UserServicesImplTest {
    private static final Logger logger = LoggerFactory.getLogger(UserServicesImplTest.class);

    // Test data constants
    private static final String TEST_EMAIL = "a@example.com";
    private static final String TEST_EMAIL_MISSING = "missing@example.com";
    private static final String TEST_USER_ID_1 = "u1";
    private static final String TEST_USER_ID_MISSING = "missing";
    private static final String TEST_PASSWORD = "pass";
    private static final String TEST_PASSWORD_BAD = "bad";
    private static final String TEST_JWT_TOKEN = "JWT_TOKEN";

    @Mock
    private UserRepositories userRepositories;

    @Mock
    private JWTServices jwtServices;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserServicesImpl userServices;

    private static User user(String id, String email, String password) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setPassword(password);
        return u;
    }

    @Test
    void login_success_returnsToken() {
        User u = user(TEST_USER_ID_1, TEST_EMAIL, TEST_PASSWORD);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(jwtServices.generateToken(authentication, TEST_USER_ID_1)).thenReturn(TEST_JWT_TOKEN);

        String token = userServices.login(u);
        assertThat(token).isEqualTo(TEST_JWT_TOKEN);
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtServices, times(1)).generateToken(authentication, TEST_USER_ID_1);
        logger.info("✅ USER/SERVICE: login_success_returnsToken() passed successfully.");
    }

    @Test
    void login_authenticationFails_throwsCredentialsNotFound() {
        User u = user(TEST_USER_ID_1, TEST_EMAIL, TEST_PASSWORD_BAD);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new AuthenticationException("bad creds"){});

        assertThatThrownBy(() -> userServices.login(u))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
            .hasMessageContaining("Invalid username or password");
        logger.info("✅ USER/SERVICE: login_authenticationFails_throwsCredentialsNotFound() passed successfully.");
    }

    @Test
    void findByEmail_found_returnsUser() {
        when(userRepositories.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user(TEST_USER_ID_1, TEST_EMAIL, "p")));
        User result = userServices.findByEmail(TEST_EMAIL);
        assertThat(result.getId()).isEqualTo(TEST_USER_ID_1);
        logger.info("✅ USER/SERVICE: findByEmail_found_returnsUser() passed successfully.");
    }

    @Test
    void findByEmail_notFound_throws() {
        when(userRepositories.findByEmail(TEST_EMAIL_MISSING)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userServices.findByEmail(TEST_EMAIL_MISSING))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
            .hasMessageContaining("User not found with email");
        logger.info("✅ USER/SERVICE: findByEmail_notFound_throws() passed successfully.");
    }

    @Test
    void findById_found_returnsUser() {
        when(userRepositories.findById(TEST_USER_ID_1)).thenReturn(Optional.of(user(TEST_USER_ID_1, TEST_EMAIL, "p")));
        User result = userServices.findById(TEST_USER_ID_1);
        assertThat(result.getEmail()).isEqualTo(TEST_EMAIL);
        logger.info("✅ USER/SERVICE: findById_found_returnsUser() passed successfully.");
    }

    @Test
    void findById_notFound_throws() {
        when(userRepositories.findById(TEST_USER_ID_MISSING)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userServices.findById(TEST_USER_ID_MISSING))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
            .hasMessageContaining("User not found with id");
        logger.info("✅ USER/SERVICE: findById_notFound_throws() passed successfully.");
    }

    @Test
    void findAllUsers_returnsList() {
        when(userRepositories.findAll()).thenReturn(List.of(user(TEST_USER_ID_1, TEST_EMAIL, "p")));
        List<User> all = userServices.findAllUsers();
        assertThat(all).hasSize(1);
        verify(userRepositories, times(1)).findAll();
        logger.info("✅ USER/SERVICE: findAllUsers_returnsList() passed successfully.");
    }
} 