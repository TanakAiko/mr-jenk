package sn.dev.user_service.services;

import sn.dev.user_service.data.entities.User;

import java.util.List;


public interface UserServices {
    String login(User user);
    User findByEmail(String email);
    User findById(String id);
    List<User> findAllUsers();
}
