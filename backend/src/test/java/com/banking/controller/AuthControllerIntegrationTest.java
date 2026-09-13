package com.banking.controller;

import com.banking.dto.LoginRequest;
import com.banking.dto.RegisterRequest;
import com.banking.enums.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Integration: Should register and then login successfully")
    void testRegisterAndLoginIntegration() throws Exception {
        String testEmail = "testuser" + System.currentTimeMillis() + "@example.com";

        RegisterRequest registerRequest = RegisterRequest.builder()
                .name("Integration Test User")
                .email(testEmail)
                .password("TestSecret@123")
                .phone("9123456780")
                .role(Role.CUSTOMER)
                .build();

        // 1. Register API
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value(testEmail));

        // 2. Login API
        LoginRequest loginRequest = LoginRequest.builder()
                .email(testEmail)
                .password("TestSecret@123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.name").value("Integration Test User"));
    }

    @Test
    @DisplayName("Integration: Should reject login with wrong password")
    void testLoginWithWrongPasswordFails() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("admin@bank.com")
                .password("WrongPassword@999")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
