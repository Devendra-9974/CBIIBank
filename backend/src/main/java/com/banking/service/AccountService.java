package com.banking.service;

import com.banking.dto.AccountResponse;
import com.banking.dto.BalanceResponse;
import com.banking.dto.CreateAccountRequest;
import com.banking.entity.Account;
import com.banking.entity.User;
import com.banking.enums.AccountStatus;
import com.banking.enums.Role;
import com.banking.exception.AccountNotFoundException;
import com.banking.exception.InvalidTransactionException;
import com.banking.exception.UnauthorizedException;
import com.banking.exception.UserNotFoundException;
import com.banking.repository.AccountRepository;
import com.banking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public AccountResponse createAccount(Long userId, CreateAccountRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        BigDecimal initialDeposit = request.getInitialDeposit() != null ? request.getInitialDeposit() : BigDecimal.ZERO;
        if (initialDeposit.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidTransactionException("Initial deposit cannot be negative");
        }

        String accountNumber = generateUniqueAccountNumber();

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .accountType(request.getAccountType())
                .balance(initialDeposit)
                .status(AccountStatus.ACTIVE)
                .user(user)
                .build();

        Account saved = accountRepository.save(account);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long accountId, Long currentUserId, Role currentRole) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + accountId));

        if (currentRole == Role.CUSTOMER && !account.getUser().getId().equals(currentUserId)) {
            throw new UnauthorizedException("You are not authorized to view this account");
        }

        return mapToResponse(account);
    }

    @Transactional(readOnly = true)
    public BalanceResponse getAccountBalance(Long accountId, Long currentUserId, Role currentRole) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + accountId));

        if (currentRole == Role.CUSTOMER && !account.getUser().getId().equals(currentUserId)) {
            throw new UnauthorizedException("You are not authorized to view this account's balance");
        }

        return BalanceResponse.builder()
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .status(account.getStatus())
                .build();
    }

    @Transactional
    public AccountResponse closeAccount(Long accountId, Long currentUserId, Role currentRole) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + accountId));

        if (currentRole == Role.CUSTOMER && !account.getUser().getId().equals(currentUserId)) {
            throw new UnauthorizedException("You are not authorized to close this account");
        }

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new InvalidTransactionException("Account is already closed");
        }

        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new InvalidTransactionException("Account balance must be 0.00 before closing. Current balance: " + account.getBalance());
        }

        account.setStatus(AccountStatus.CLOSED);
        Account saved = accountRepository.save(account);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;
        do {
            long number = 100000000000L + (long) (secureRandom.nextDouble() * 899999999999L);
            accountNumber = String.valueOf(number);
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }

    public AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .userId(account.getUser().getId())
                .userName(account.getUser().getName())
                .userEmail(account.getUser().getEmail())
                .build();
    }
}
