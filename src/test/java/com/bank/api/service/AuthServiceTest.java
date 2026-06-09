package com.bank.api.service;

import com.bank.api.domain.User;
import com.bank.api.dto.request.RegisterRequest;
import com.bank.api.dto.response.AuthResponse;
import com.bank.api.exception.ConflictException;
import com.bank.api.repository.UserRepository;
import com.bank.api.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    // DECISION: Manual instantiation because AuthService has a @Value constructor
    // parameter that Mockito's @InjectMocks cannot resolve without Spring context.
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtService,
                authenticationManager,
                3600000L   // 1 hour expiration — matches application.yml
        );
    }

    @Test
    @DisplayName("Registration with valid details should return a JWT token")
    void register_validRequest_shouldReturnToken() {
        RegisterRequest request = new RegisterRequest(
                "John", "Doe", "john@example.com", "password123"
        );
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            // Simulate JPA setting the ID on save
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(u, UUID.randomUUID());
            return u;
        });
        when(jwtService.generateToken(any(UUID.class), eq("john@example.com")))
                .thenReturn("mock.jwt.token");

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("mock.jwt.token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("Registration should hash the password before saving")
    void register_shouldHashPasswordBeforeSaving() {
        RegisterRequest request = new RegisterRequest(
                "John", "Doe", "john@example.com", "plainTextPassword"
        );
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("plainTextPassword")).thenReturn("$2a$12$hashedValue");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(u, UUID.randomUUID());
            return u;
        });
        when(jwtService.generateToken(any(), anyString())).thenReturn("token");

        authService.register(request);

        verify(passwordEncoder).encode("plainTextPassword");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Registration with duplicate email should throw ConflictException")
    void register_duplicateEmail_shouldThrowConflictException() {
        RegisterRequest request = new RegisterRequest(
                "Jane", "Doe", "existing@example.com", "password123"
        );
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Registration with duplicate email should never attempt to save the user")
    void register_duplicateEmail_shouldNeverSave() {
        RegisterRequest request = new RegisterRequest(
                "Jane", "Doe", "taken@example.com", "password123"
        );
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }
}