package com.banking.service;

import com.banking.dto.AuthResponse;
import com.banking.dto.LoginRequest;
import com.banking.dto.RegisterRequest;
import com.banking.entity.Account;
import com.banking.entity.User;
import com.banking.enums.AccountStatus;
import com.banking.enums.AccountType;
import com.banking.enums.Role;
import com.banking.exception.DuplicateUserException;
import com.banking.repository.AccountRepository;
import com.banking.repository.UserRepository;
import com.banking.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new DuplicateUserException("Email is already registered: " + request.getEmail());
        }

        Role userRole = request.getRole() != null ? request.getRole() : Role.CUSTOMER;

        User user = User.builder()
                .name(request.getName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(userRole)
                .build();

        User savedUser = userRepository.save(user);

        // If newly registered user is a CUSTOMER, automatically create a default SAVINGS account with initial welcome balance
        if (userRole == Role.CUSTOMER) {
            String accountNumber = generateUniqueAccountNumber();
            Account defaultAccount = Account.builder()
                    .accountNumber(accountNumber)
                    .accountType(AccountType.SAVINGS)
                    .balance(new BigDecimal("1000.00")) // Welcome credit
                    .status(AccountStatus.ACTIVE)
                    .user(savedUser)
                    .build();
            accountRepository.save(defaultAccount);
        }

        String token = jwtUtils.generateTokenFromEmail(
                savedUser.getEmail(),
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getRole().name()
        );

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().trim().toLowerCase(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return AuthResponse.builder()
                .token(jwt)
                .type("Bearer")
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;
        do {
            long number = 100000000000L + (long) (secureRandom.nextDouble() * 899999999999L);
            accountNumber = String.valueOf(number);
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }
}
