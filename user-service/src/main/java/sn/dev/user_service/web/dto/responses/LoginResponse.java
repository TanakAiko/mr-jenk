package sn.dev.user_service.web.dto.responses;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class LoginResponse {
    private final String email;
    private final String token;
    private List<String> role = new ArrayList<>();

    public LoginResponse(UserDetails userDetails, String token) {
        email= userDetails.getUsername();
        this.token = token;
        var formatted_role = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(" "));
        role.add(formatted_role);
    }
}
