package sn.dev.user_service.web.dto.requests;

import sn.dev.user_service.data.entities.Role;
import sn.dev.user_service.data.entities.User;

public class UserRequests {
    private String name;
    private String email;
    private String password;
    private String role;

    public User toEntity() {
        User user = new User();
        user.setRole(Role.valueOf(this.role));
        user.setPassword(this.password);
        user.setEmail(this.email);
        user.setName(this.name);
        return user;
    }
}
