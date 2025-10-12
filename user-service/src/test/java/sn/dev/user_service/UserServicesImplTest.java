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
        User u = user("u1", "a@example.com", "pass");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(jwtServices.generateToken(authentication, "u1")).thenReturn("JWT_TOKEN");

        String token = userServices.login(u);
        assertThat(token).isEqualTo("JWT_TOKEN");
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtServices, times(1)).generateToken(authentication, "u1");
        System.out.println("✅ USER/SERVICE: login_success_returnsToken() passed successfully.");
    }

    @Test
    void login_authenticationFails_throwsCredentialsNotFound() {
        User u = user("u1", "a@example.com", "bad");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new AuthenticationException("bad creds"){});

        assertThatThrownBy(() -> userServices.login(u))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
            .hasMessageContaining("Invalid username or password");
        System.out.println("✅ USER/SERVICE: login_authenticationFails_throwsCredentialsNotFound() passed successfully.");
    }

    @Test
    void findByEmail_found_returnsUser() {
        when(userRepositories.findByEmail("a@example.com")).thenReturn(Optional.of(user("u1", "a@example.com", "p")));
        User result = userServices.findByEmail("a@example.com");
        assertThat(result.getId()).isEqualTo("u1");
        System.out.println("✅ USER/SERVICE: findByEmail_found_returnsUser() passed successfully.");
    }

    @Test
    void findByEmail_notFound_throws() {
        when(userRepositories.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userServices.findByEmail("missing@example.com"))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
            .hasMessageContaining("User not found with email");
        System.out.println("✅ USER/SERVICE: findByEmail_notFound_throws() passed successfully.");
    }

    @Test
    void findById_found_returnsUser() {
        when(userRepositories.findById("u1")).thenReturn(Optional.of(user("u1", "a@example.com", "p")));
        User result = userServices.findById("u1");
        assertThat(result.getEmail()).isEqualTo("a@example.com");
        System.out.println("✅ USER/SERVICE: findById_found_returnsUser() passed successfully.");
    }

    @Test
    void findById_notFound_throws() {
        when(userRepositories.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userServices.findById("missing"))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
            .hasMessageContaining("User not found with id");
        System.out.println("✅ USER/SERVICE: findById_notFound_throws() passed successfully.");
    }

    @Test
    void findAllUsers_returnsList() {
        when(userRepositories.findAll()).thenReturn(List.of(user("u1", "a@example.com", "p")));
        List<User> all = userServices.findAllUsers();
        assertThat(all).hasSize(1);
        verify(userRepositories, times(1)).findAll();
        System.out.println("✅ USER/SERVICE: findAllUsers_returnsList() passed successfully.");
    }
} 