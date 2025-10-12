package sn.dev.user_service.web.controllers.impl;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;
import lombok.AllArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;
import sn.dev.user_service.data.entities.User;
import sn.dev.user_service.services.UserServices;
import sn.dev.user_service.web.controllers.UserControllers;
import sn.dev.user_service.web.dto.requests.LoginRequests;
import sn.dev.user_service.web.dto.responses.LoginResponse;
import sn.dev.user_service.web.dto.responses.UserResponse;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
public class UserControllersImpl implements UserControllers {
    private final UserServices userServices;
    private final UserDetailsService userDetailsService;

    @Override
    @PostMapping("api/users/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequests loginRequests) {
        String userID = userServices.findByEmail(loginRequests.getEmail()).getId();
        User credentialsUser = loginRequests.toEntity();
        credentialsUser.setId(userID);
        String token = userServices.login(credentialsUser);
        UserDetails userDetails = userDetailsService.loadUserByUsername(credentialsUser.getEmail());
        LoginResponse loginResponse = new LoginResponse(userDetails, token);
        return ResponseEntity.ok(loginResponse);
    }

    @Override
    @GetMapping("api/users/{userID}/custom")
    public ResponseEntity<UserResponse> getUser(@PathVariable String userID) {
        User user = userServices.findById(userID);
        UserResponse userResponse = new UserResponse(user);
        userResponse.add(linkTo(methodOn(this.getClass()).getUser(userID)).withSelfRel());
        return ResponseEntity.ok(userResponse);
    }

    @Override
    @GetMapping("api/users/custom")
    public ResponseEntity<CollectionModel<UserResponse>> getUsers() {
        List<User> users = userServices.findAllUsers();

        List<UserResponse> userDtos = users.stream()
                .map(user -> {
                    UserResponse dto = new UserResponse(user);
                    dto.add(linkTo(methodOn(UserControllersImpl.class)
                            .getUser(user.getId())) // Assurez-vous que la méthode getUser(id) existe
                            .withSelfRel());
                    return dto;
                })
                .collect(Collectors.toList());

        // 3. Créer le CollectionModel et lui ajouter le lien "self" de la collection
        CollectionModel<UserResponse> collectionModel = CollectionModel.of(userDtos,
                linkTo(methodOn(UserControllersImpl.class).getUsers()).withSelfRel()
        );

        System.out.println("\n\ncollectionModel = " + collectionModel);
        return ResponseEntity.ok(collectionModel);
    }
}
