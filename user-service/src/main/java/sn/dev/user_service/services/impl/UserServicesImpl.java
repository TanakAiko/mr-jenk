package sn.dev.user_service.services.impl;

import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import sn.dev.user_service.data.entities.User;
import sn.dev.user_service.data.repositories.UserRepositories;
import sn.dev.user_service.services.JWTServices;
import sn.dev.user_service.services.UserServices;

import java.util.Arrays;
import java.util.List;

@Service
@AllArgsConstructor
public class UserServicesImpl implements UserServices {

    private final UserRepositories userRepositories;
    private final JWTServices jwtServices;
    private final AuthenticationManager authenticationManager;

    @Override
    public String login(User user) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword()));
            if (authentication.isAuthenticated()) {
                return jwtServices.generateToken(authentication, user.getId());
            }
        } catch (AuthenticationException e) {
            System.out.println(e.getMessage());
            System.out.println(user);
            System.out.println(Arrays.stream(e.getStackTrace()).toList());
            throw new AuthenticationCredentialsNotFoundException("Invalid username or password");
        }
        return "fail";
    }

    @Override
    public User findByEmail(String email) {
        return userRepositories.findByEmail(email)
                .orElseThrow(
                        () -> new AuthenticationCredentialsNotFoundException("User not found with email: " + email));
    }

    @Override
    public User findById(String id) {
        return userRepositories.findById(id)
                .orElseThrow(
                        () -> new AuthenticationCredentialsNotFoundException("User not found with id: " + id));
    }

    @Override
    public List<User> findAllUsers() {
        return userRepositories.findAll();
    }
}
