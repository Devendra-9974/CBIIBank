package com.banking.service;

import com.banking.dto.DepositRequest;
import com.banking.dto.TransactionResponse;
import com.banking.dto.TransferRequest;
import com.banking.dto.WithdrawRequest;
import com.banking.entity.Account;
import com.banking.entity.Transaction;
import com.banking.entity.User;
import com.banking.enums.AccountStatus;
import com.banking.enums.AccountType;
import com.banking.enums.Role;
import com.banking.enums.TransactionType;
import com.banking.exception.InsufficientBalanceException;
import com.banking.exception.InvalidTransactionException;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User senderUser;
    private User receiverUser;
    private Account sourceAccount;
    private Account targetAccount;

    @BeforeEach
    void setUp() {
        senderUser = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .role(Role.CUSTOMER)
                .build();

        receiverUser = User.builder()
                .id(2L)
                .name("Jane Smith")
                .email("jane@example.com")
                .role(Role.CUSTOMER)
                .build();

        sourceAccount = Account.builder()
                .id(101L)
                .accountNumber("100100200300")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("10000.00"))
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .user(senderUser)
                .build();

        targetAccount = Account.builder()
                .id(102L)
                .accountNumber("200200300400")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("5000.00"))
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .user(receiverUser)
                .build();
    }

    @Test
    @DisplayName("Should successfully deposit money into an active account")
    void testDepositSuccess() {
        DepositRequest request = DepositRequest.builder()
                .accountNumber("100100200300")
                .amount(new BigDecimal("2000.00"))
                .description("ATM Cash Deposit")
                .build();

        when(accountRepository.findByAccountNumberWithLock("100100200300")).thenReturn(Optional.of(sourceAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> {
            Transaction tx = i.getArgument(0);
            tx.setId(501L);
            tx.setTimestamp(LocalDateTime.now());
            return tx;
        });

        TransactionResponse response = transactionService.deposit(request, 1L, Role.CUSTOMER);

        assertNotNull(response);
        assertEquals(new BigDecimal("12000.00"), sourceAccount.getBalance());
        assertEquals(TransactionType.DEPOSIT, response.getType());
        assertEquals(new BigDecimal("2000.00"), response.getAmount());
    }

    @Test
    @DisplayName("Should successfully withdraw money when sufficient balance exists")
    void testWithdrawSuccess() {
        WithdrawRequest request = WithdrawRequest.builder()
                .accountNumber("100100200300")
                .amount(new BigDecimal("3000.00"))
                .description("Branch Withdrawal")
                .build();

        when(accountRepository.findByAccountNumberWithLock("100100200300")).thenReturn(Optional.of(sourceAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> {
            Transaction tx = i.getArgument(0);
            tx.setId(502L);
            tx.setTimestamp(LocalDateTime.now());
            return tx;
        });

        TransactionResponse response = transactionService.withdraw(request, 1L, Role.CUSTOMER);

        assertNotNull(response);
        assertEquals(new BigDecimal("7000.00"), sourceAccount.getBalance());
        assertEquals(TransactionType.WITHDRAWAL, response.getType());
        assertEquals(new BigDecimal("3000.00"), response.getAmount());
    }

    @Test
    @DisplayName("Should throw InsufficientBalanceException when withdrawal amount exceeds balance")
    void testWithdrawInsufficientBalance() {
        WithdrawRequest request = WithdrawRequest.builder()
                .accountNumber("100100200300")
                .amount(new BigDecimal("15000.00"))
                .build();

        when(accountRepository.findByAccountNumberWithLock("100100200300")).thenReturn(Optional.of(sourceAccount));

        assertThrows(InsufficientBalanceException.class, () ->
                transactionService.withdraw(request, 1L, Role.CUSTOMER)
        );
        assertEquals(new BigDecimal("10000.00"), sourceAccount.getBalance());
    }

    @Test
    @DisplayName("Should atomically transfer money from source to target account")
    void testAtomicTransferSuccess() {
        TransferRequest request = TransferRequest.builder()
                .sourceAccountNumber("100100200300")
                .targetAccountNumber("200200300400")
                .amount(new BigDecimal("4000.00"))
                .description("Consulting fee payment")
                .build();

        when(accountRepository.findByAccountNumberWithLock("100100200300")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumberWithLock("200200300400")).thenReturn(Optional.of(targetAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> {
            Transaction tx = i.getArgument(0);
            tx.setId(503L);
            tx.setTimestamp(LocalDateTime.now());
            return tx;
        });

        TransactionResponse response = transactionService.transfer(request, 1L, Role.CUSTOMER);

        assertNotNull(response);
        assertEquals(new BigDecimal("6000.00"), sourceAccount.getBalance());
        assertEquals(new BigDecimal("9000.00"), targetAccount.getBalance());
        assertEquals(TransactionType.TRANSFER, response.getType());
        assertEquals(new BigDecimal("4000.00"), response.getAmount());
        verify(accountRepository, times(1)).save(sourceAccount);
        verify(accountRepository, times(1)).save(targetAccount);
    }

    @Test
    @DisplayName("Should throw InvalidTransactionException if source and destination are identical")
    void testTransferSameAccountFails() {
        TransferRequest request = TransferRequest.builder()
                .sourceAccountNumber("100100200300")
                .targetAccountNumber("100100200300")
                .amount(new BigDecimal("500.00"))
                .build();

        assertThrows(InvalidTransactionException.class, () ->
                transactionService.transfer(request, 1L, Role.CUSTOMER)
        );
    }

    @Test
    @DisplayName("Should throw InsufficientBalanceException if transfer amount exceeds balance")
    void testTransferInsufficientBalanceFails() {
        TransferRequest request = TransferRequest.builder()
                .sourceAccountNumber("100100200300")
                .targetAccountNumber("200200300400")
                .amount(new BigDecimal("25000.00"))
                .build();

        when(accountRepository.findByAccountNumberWithLock("100100200300")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumberWithLock("200200300400")).thenReturn(Optional.of(targetAccount));

        assertThrows(InsufficientBalanceException.class, () ->
                transactionService.transfer(request, 1L, Role.CUSTOMER)
        );
        assertEquals(new BigDecimal("10000.00"), sourceAccount.getBalance());
        assertEquals(new BigDecimal("5000.00"), targetAccount.getBalance());
    }

    @Test
    @DisplayName("Should throw InvalidTransactionException when transferring from blocked account")
    void testTransferFromBlockedAccountFails() {
        sourceAccount.setStatus(AccountStatus.BLOCKED);

        TransferRequest request = TransferRequest.builder()
                .sourceAccountNumber("100100200300")
                .targetAccountNumber("200200300400")
                .amount(new BigDecimal("1000.00"))
                .build();

        when(accountRepository.findByAccountNumberWithLock("100100200300")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumberWithLock("200200300400")).thenReturn(Optional.of(targetAccount));

        assertThrows(InvalidTransactionException.class, () ->
                transactionService.transfer(request, 1L, Role.CUSTOMER)
        );
    }
}
