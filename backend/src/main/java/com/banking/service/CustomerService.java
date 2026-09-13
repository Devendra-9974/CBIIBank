package com.banking.service;

import com.banking.dto.AccountResponse;
import com.banking.dto.ChangePasswordRequest;
import com.banking.dto.UpdateProfileRequest;
import com.banking.dto.UserProfileResponse;
import com.banking.entity.Account;
import com.banking.entity.User;
import com.banking.exception.InvalidTransactionException;
import com.banking.exception.UserNotFoundException;
import com.banking.repository.AccountRepository;
import com.banking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        user.setName(request.getName().trim());
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }

        User savedUser = userRepository.save(user);

        return UserProfileResponse.builder()
                .id(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .phone(savedUser.getPhone())
                .role(savedUser.getRole())
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new InvalidTransactionException("Current password does not match");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new InvalidTransactionException("New password cannot be the same as old password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getCustomerAccounts(Long userId) {
        List<Account> accounts = accountRepository.findByUserId(userId);
        return accounts.stream()
                .map(acc -> AccountResponse.builder()
                        .id(acc.getId())
                        .accountNumber(acc.getAccountNumber())
                        .accountType(acc.getAccountType())
                        .balance(acc.getBalance())
                        .status(acc.getStatus())
                        .createdAt(acc.getCreatedAt())
                        .userId(acc.getUser().getId())
                        .userName(acc.getUser().getName())
                        .userEmail(acc.getUser().getEmail())
                        .build())
                .collect(Collectors.toList());
    }
}
