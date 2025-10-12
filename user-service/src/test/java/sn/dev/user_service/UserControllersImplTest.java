package sn.dev.user_service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import sn.dev.user_service.data.entities.*;
import sn.dev.user_service.services.UserServices;
import sn.dev.user_service.web.dto.requests.LoginRequests;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllersImplTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserServices userServices;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static User user(String id, String name, String email, String password) {
        User u = new User();
        u.setId(id);
        u.setName(name);
        u.setEmail(email);
        u.setPassword(password);
        return u;
    }

    private static LoginRequests loginRequest(String email, String password) {
        LoginRequests lg = new LoginRequests();
        lg.setEmail(email);
        lg.setPassword(password);
        return lg;
    }

    @Test
    @WithMockUser(username = "john@example.com", roles = "USER")
    void testLogin_Success() throws Exception {
        LoginRequests loginReq = loginRequest("john@example.com", "password");
        User foundUser = user("1", "John Doe", "john@example.com", "hashedPass");

        // mock services
        when(userServices.findByEmail("john@example.com")).thenReturn(foundUser);
        when(userServices.login(any(User.class))).thenReturn("fake-jwt-token");
        UserDetails springUser = org.springframework.security.core.userdetails.User.withUsername("john@example.com")
                .password("hashedPass")
                .roles("USER")
                .build();
        when(userDetailsService.loadUserByUsername("john@example.com")).thenReturn(springUser);

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake-jwt-token"))
                .andExpect(jsonPath("$.email").value("john@example.com"));

        System.out.println("✅ USER/CONTROLLER : testLogin_Success() passed successfully.");
    }

    @Test
    @WithMockUser
    void testGetUserById_Success() throws Exception {
        User user = user("1", "John Doe", "john@example.com", "hashedPass");
        user.setRole(Role.CLIENT);
        
        when(userServices.findById("1")).thenReturn(user);

        mockMvc.perform(get("/api/users/{userID}/custom", "1")
                .accept(MediaTypes.HAL_JSON)) // because HATEOAS adds _links
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$._links.self.href").exists());

        System.out.println("✅ USER/CONTROLLER : testGetUserById_Success() passed successfully.");
    }

    @Test
    @WithMockUser
    void testGetUsers_Success() throws Exception {
        User user1 = user("1", "Alice Smith", "alice@example.com", "pass1");
        user1.setRole(Role.CLIENT);
        User user2 = user("2", "Bob Jones", "bob@example.com", "pass2");
        user2.setRole(Role.CLIENT);

        when(userServices.findAllUsers()).thenReturn(List.of(user1, user2));

        mockMvc.perform(get("/api/users/custom")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.userResponses[0].id").value("1"))
                .andExpect(jsonPath("$._embedded.userResponses[0].email").value("alice@example.com"))
                .andExpect(jsonPath("$._embedded.userResponses[1].id").value("2"))
                .andExpect(jsonPath("$._embedded.userResponses[1].email").value("bob@example.com"))
                .andExpect(jsonPath("$._links.self.href").exists());

        System.out.println("✅ USER/CONTROLLER : testGetUsers_Success() passed successfully.");
    }
}
