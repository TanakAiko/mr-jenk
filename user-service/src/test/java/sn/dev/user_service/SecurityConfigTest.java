package sn.dev.user_service;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import sn.dev.user_service.services.UserServices;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserServices userServices;

    @Test
    void testProtectedEndpoint_GetUsers_WithoutAuth_ShouldBeDenied() throws Exception {
        // Test that protected endpoint requires authentication
        mockMvc.perform(get("/api/users")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        System.out.println("✅ SECURITY: testProtectedEndpoint_GetUsers_WithoutAuth_ShouldBeDenied() passed.");
    }

    @Test
    void testProtectedEndpoint_GetUserById_WithoutAuth_ShouldBeDenied() throws Exception {
        // Test that getting user by ID requires authentication
        mockMvc.perform(get("/api/users/123")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        System.out.println("✅ SECURITY: testProtectedEndpoint_GetUserById_WithoutAuth_ShouldBeDenied() passed.");
    }

    @Test
    void testProtectedEndpoint_UpdateUser_WithoutAuth_ShouldBeDenied() throws Exception {
        // Test that PUT endpoint requires authentication
        String updateJson = "{\"name\":\"Updated Name\"}";

        mockMvc.perform(put("/api/users/123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isUnauthorized());

        System.out.println("✅ SECURITY: testProtectedEndpoint_UpdateUser_WithoutAuth_ShouldBeDenied() passed.");
    }

    @Test
    void testProtectedEndpoint_PatchUser_WithoutAuth_ShouldBeDenied() throws Exception {
        // Test that PATCH endpoint requires authentication
        String patchJson = "{\"name\":\"Patched Name\"}";

        mockMvc.perform(patch("/api/users/123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(patchJson))
                .andExpect(status().isUnauthorized());

        System.out.println("✅ SECURITY: testProtectedEndpoint_PatchUser_WithoutAuth_ShouldBeDenied() passed.");
    }

    @Test
    void testProtectedEndpoint_DeleteUser_WithoutAuth_ShouldBeDenied() throws Exception {
        // Test that DELETE endpoint requires authentication
        mockMvc.perform(delete("/api/users/123"))
                .andExpect(status().isUnauthorized());

        System.out.println("✅ SECURITY: testProtectedEndpoint_DeleteUser_WithoutAuth_ShouldBeDenied() passed.");
    }

    @Test
    void testProtectedEndpoint_CreateProduct_WithoutAuth_ShouldBeDenied() throws Exception {
        // Test that product creation requires authentication
        String productJson = "{\"name\":\"Test Product\",\"price\":100}";

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson))
                .andExpect(status().isUnauthorized());

        System.out.println("✅ SECURITY: testProtectedEndpoint_CreateProduct_WithoutAuth_ShouldBeDenied() passed.");
    }
}
