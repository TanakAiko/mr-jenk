package sn.dev.user_service.web.controllers;

import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;
import sn.dev.user_service.web.dto.requests.LoginRequests;
import sn.dev.user_service.web.dto.responses.LoginResponse;
import sn.dev.user_service.web.dto.responses.UserResponse;


public interface UserControllers {
    ResponseEntity<LoginResponse> login(LoginRequests loginRequests);
    ResponseEntity<UserResponse> getUser(String userID);
    ResponseEntity<CollectionModel<UserResponse>> getUsers();
}
