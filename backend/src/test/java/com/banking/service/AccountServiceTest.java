package com.banking.service;

import com.banking.dto.AccountResponse;
import com.banking.dto.BalanceResponse;
import com.banking.dto.CreateAccountRequest;
import com.banking.entity.Account;
import com.banking.entity.User;
import com.banking.enums.AccountStatus;
import com.banking.enums.AccountType;
import com.banking.enums.Role;
import com.banking.exception.InvalidTransactionException;
import com.banking.exception.UnauthorizedException;
import com.banking.repository.AccountRepository;
import com.banking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountService accountService;

    private User sampleUser;
    private Account sampleAccount;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .role(Role.CUSTOMER)
                .build();

        sampleAccount = Account.builder()
                .id(100L)
                .accountNumber("100100200300")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("5000.00"))
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .user(sampleUser)
                .build();
    }

    @Test
    @DisplayName("Should create a new bank account successfully")
    void testCreateAccount() {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .accountType(AccountType.SAVINGS)
                .initialDeposit(new BigDecimal("500.00"))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> {
            Account acc = i.getArgument(0);
            acc.setId(101L);
            acc.setCreatedAt(LocalDateTime.now());
            return acc;
        });

        AccountResponse response = accountService.createAccount(1L, request);

        assertNotNull(response);
        assertEquals(new BigDecimal("500.00"), response.getBalance());
        assertEquals(AccountType.SAVINGS, response.getAccountType());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    @DisplayName("Should get balance when requested by owner")
    void testGetBalanceByOwner() {
        when(accountRepository.findById(100L)).thenReturn(Optional.of(sampleAccount));

        BalanceResponse response = accountService.getAccountBalance(100L, 1L, Role.CUSTOMER);

        assertNotNull(response);
        assertEquals(new BigDecimal("5000.00"), response.getBalance());
        assertEquals("100100200300", response.getAccountNumber());
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when requested by another customer")
    void testGetBalanceUnauthorized() {
        when(accountRepository.findById(100L)).thenReturn(Optional.of(sampleAccount));

        assertThrows(UnauthorizedException.class, () ->
                accountService.getAccountBalance(100L, 2L, Role.CUSTOMER)
        );
    }

    @Test
    @DisplayName("Should fail to close account if balance is greater than zero")
    void testCloseAccountWithBalanceFails() {
        when(accountRepository.findById(100L)).thenReturn(Optional.of(sampleAccount));

        assertThrows(InvalidTransactionException.class, () ->
                accountService.closeAccount(100L, 1L, Role.CUSTOMER)
        );
    }

    @Test
    @DisplayName("Should successfully close account if balance is zero")
    void testCloseAccountWithZeroBalanceSuccess() {
        sampleAccount.setBalance(BigDecimal.ZERO);
        when(accountRepository.findById(100L)).thenReturn(Optional.of(sampleAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

        AccountResponse response = accountService.closeAccount(100L, 1L, Role.CUSTOMER);

        assertNotNull(response);
        assertEquals(AccountStatus.CLOSED, response.getStatus());
    }
}
