package com.bank.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("Auth Controller Integration Tests")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String REGISTER_URL = "/v1/auth/register";
    private static final String LOGIN_URL = "/v1/auth/login";
    private static final String ME_URL = "/v1/users";

    @Test
    @DisplayName("POST /auth/register with valid details should return 201 and a token")
    void register_validRequest_returns201WithToken() throws Exception {
        String requestBody = """
                {
                    "first_name": "John",
                    "last_name": "Doe",
                    "email": "john@example.com",
                    "password": "password123"
                }
                """;
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").isNumber());
    }

    @Test
    @DisplayName("POST /auth/register with duplicate email should return 409 Conflict")
    void register_duplicateEmail_returns409() throws Exception {
        String requestBody = """
                {
                    "first_name": "John",
                    "last_name": "Doe",
                    "email": "duplicate@example.com",
                    "password": "password123"
                }
                """;
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("An account with this email already exists"));
    }

    @Test
    @DisplayName("POST /auth/register with missing fields should return 400 with field errors")
    void register_missingFields_returns400WithFieldErrors() throws Exception {
        String requestBody = """
                {
                    "email": "john@example.com"
                }
                """;
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    @DisplayName("POST /auth/register with invalid email should return 400")
    void register_invalidEmail_returns400() throws Exception {
        String requestBody = """
                {
                    "first_name": "John",
                    "last_name": "Doe",
                    "email": "not-an-email",
                    "password": "password123"
                }
                """;
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/login with valid credentials should return 200 and a token")
    void login_validCredentials_returns200WithToken() throws Exception {
        String registerBody = """
                {
                    "first_name": "John",
                    "last_name": "Doe",
                    "email": "john@example.com",
                    "password": "password123"
                }
                """;
        mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody));

        String loginBody = """
                {
                    "email": "john@example.com",
                    "password": "password123"
                }
                """;
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.token_type").value("Bearer"));
    }

    @Test
    @DisplayName("POST /auth/login with wrong password should return 401")
    void login_wrongPassword_returns401() throws Exception {
        String registerBody = """
                {
                    "first_name": "John",
                    "last_name": "Doe",
                    "email": "john@example.com",
                    "password": "correctPassword"
                }
                """;
        mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody));

        String loginBody = """
                {
                    "email": "john@example.com",
                    "password": "wrongPassword"
                }
                """;
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    @DisplayName("GET /users/{userId} without token should return 401")
    void getMe_noToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/users/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /users/{userId} with valid token should return user profile")
    void getMe_validToken_returnsUserProfile() throws Exception {
        String registerBody = """
            {
                "first_name": "John",
                "last_name": "Doe",
                "email": "john@example.com",
                "password": "password123"
            }
            """;

        MvcResult registerResult = mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andReturn();

        String responseJson = registerResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseJson).get("token").asText();

        // Extract userId from JWT subject claim
        String[] parts = token.split("\\.");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
        String userId = objectMapper.readTree(payload).get("sub").asText();

        mockMvc.perform(get("/v1/users/" + userId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.first_name").value("John"))
                .andExpect(jsonPath("$.last_name").value("Doe"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }
}