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

    // Test data constants
    private static final String TEST_EMAIL_JOHN = "john@example.com";
    private static final String TEST_EMAIL_ALICE = "alice@example.com";
    private static final String TEST_EMAIL_BOB = "bob@example.com";
    private static final String TEST_PASSWORD = "password";
    private static final String TEST_HASHED_PASS = "hashedPass";
    private static final String TEST_USER_ID_1 = "1";
    private static final String TEST_USER_ID_2 = "2";
    private static final String TEST_NAME_JOHN = "John Doe";
    private static final String TEST_NAME_ALICE = "Alice Smith";
    private static final String TEST_NAME_BOB = "Bob Jones";
    private static final String TEST_JWT_TOKEN = "fake-jwt-token";

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
        LoginRequests loginReq = loginRequest(TEST_EMAIL_JOHN, TEST_PASSWORD);
        User foundUser = user(TEST_USER_ID_1, TEST_NAME_JOHN, TEST_EMAIL_JOHN, TEST_HASHED_PASS);

        // mock services
        when(userServices.findByEmail(TEST_EMAIL_JOHN)).thenReturn(foundUser);
        when(userServices.login(any(User.class))).thenReturn(TEST_JWT_TOKEN);
        UserDetails springUser = org.springframework.security.core.userdetails.User.withUsername(TEST_EMAIL_JOHN)
                .password(TEST_HASHED_PASS)
                .roles("USER")
                .build();
        when(userDetailsService.loadUserByUsername(TEST_EMAIL_JOHN)).thenReturn(springUser);

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(TEST_JWT_TOKEN))
                .andExpect(jsonPath("$.email").value(TEST_EMAIL_JOHN));

        System.out.println("✅ USER/CONTROLLER : testLogin_Success() passed successfully.");
    }

    @Test
    @WithMockUser
    void testGetUserById_Success() throws Exception {
        User user = user(TEST_USER_ID_1, TEST_NAME_JOHN, TEST_EMAIL_JOHN, TEST_HASHED_PASS);
        user.setRole(Role.CLIENT);
        
        when(userServices.findById(TEST_USER_ID_1)).thenReturn(user);

        mockMvc.perform(get("/api/users/{userID}/custom", TEST_USER_ID_1)
                .accept(MediaTypes.HAL_JSON)) // because HATEOAS adds _links
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEST_USER_ID_1))
                .andExpect(jsonPath("$.name").value(TEST_NAME_JOHN))
                .andExpect(jsonPath("$.email").value(TEST_EMAIL_JOHN))
                .andExpect(jsonPath("$._links.self.href").exists());

        System.out.println("✅ USER/CONTROLLER : testGetUserById_Success() passed successfully.");
    }

    @Test
    @WithMockUser
    void testGetUsers_Success() throws Exception {
        User user1 = user(TEST_USER_ID_1, TEST_NAME_ALICE, TEST_EMAIL_ALICE, "pass1");
        user1.setRole(Role.CLIENT);
        User user2 = user(TEST_USER_ID_2, TEST_NAME_BOB, TEST_EMAIL_BOB, "pass2");
        user2.setRole(Role.CLIENT);

        when(userServices.findAllUsers()).thenReturn(List.of(user1, user2));

        mockMvc.perform(get("/api/users/custom")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.userResponses[0].id").value(TEST_USER_ID_1))
                .andExpect(jsonPath("$._embedded.userResponses[0].email").value(TEST_EMAIL_ALICE))
                .andExpect(jsonPath("$._embedded.userResponses[1].id").value(TEST_USER_ID_2))
                .andExpect(jsonPath("$._embedded.userResponses[1].email").value(TEST_EMAIL_BOB))
                .andExpect(jsonPath("$._links.self.href").exists());

        System.out.println("✅ USER/CONTROLLER : testGetUsers_Success() passed successfully.");
    }
}
