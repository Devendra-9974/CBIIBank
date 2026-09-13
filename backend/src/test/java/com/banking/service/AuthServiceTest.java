package com.banking.service;

import com.banking.dto.AuthResponse;
import com.banking.dto.LoginRequest;
import com.banking.dto.RegisterRequest;
import com.banking.entity.Account;
import com.banking.entity.User;
import com.banking.enums.Role;
import com.banking.exception.DuplicateUserException;
import com.banking.repository.AccountRepository;
import com.banking.repository.UserRepository;
import com.banking.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .name("Alex Johnson")
                .email("alex@example.com")
                .password("encoded_pwd")
                .phone("9876543210")
                .role(Role.CUSTOMER)
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new customer and create default account")
    void testRegisterSuccess() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Alex Johnson")
                .email("alex@example.com")
                .password("Secret@123")
                .phone("9876543210")
                .role(Role.CUSTOMER)
                .build();

        when(userRepository.existsByEmail("alex@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret@123")).thenReturn("encoded_pwd");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtUtils.generateTokenFromEmail(anyString(), any(), anyString(), anyString())).thenReturn("mock_jwt_token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("alex@example.com", response.getEmail());
        assertEquals("Alex Johnson", response.getName());
        assertEquals("mock_jwt_token", response.getToken());
        verify(userRepository, times(1)).save(any(User.class));
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    @DisplayName("Should throw DuplicateUserException when email already exists")
    void testRegisterDuplicateEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Alex Johnson")
                .email("alex@example.com")
                .password("Secret@123")
                .build();

        when(userRepository.existsByEmail("alex@example.com")).thenReturn(true);

        assertThrows(DuplicateUserException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should login successfully and return JWT token")
    void testLoginSuccess() {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("alex@example.com")
                .password("Secret@123")
                .build();

        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtUtils.generateJwtToken(auth)).thenReturn("jwt_sample_token");
        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(sampleUser));

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("jwt_sample_token", response.getToken());
        assertEquals(1L, response.getId());
        assertEquals("Alex Johnson", response.getName());
    }
}
