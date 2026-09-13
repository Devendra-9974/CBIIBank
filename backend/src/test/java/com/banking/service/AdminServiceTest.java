package com.banking.service;

import com.banking.dto.AccountResponse;
import com.banking.dto.AdminDashboardStats;
import com.banking.dto.LoanResponse;
import com.banking.dto.UserProfileResponse;
import com.banking.entity.Account;
import com.banking.entity.Loan;
import com.banking.entity.User;
import com.banking.enums.AccountStatus;
import com.banking.enums.AccountType;
import com.banking.enums.LoanStatus;
import com.banking.enums.Role;
import com.banking.exception.InvalidTransactionException;
import com.banking.repository.AccountRepository;
import com.banking.repository.LoanRepository;
import com.banking.repository.TransactionRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private LoanService loanService;

    @InjectMocks
    private AdminService adminService;

    private User customer;
    private Account account;
    private Loan loan;

    @BeforeEach
    void setUp() {
        customer = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .role(Role.CUSTOMER)
                .createdAt(LocalDateTime.now())
                .build();

        account = Account.builder()
                .id(100L)
                .accountNumber("100100200300")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("15000.00"))
                .status(AccountStatus.ACTIVE)
                .user(customer)
                .createdAt(LocalDateTime.now())
                .build();

        loan = Loan.builder()
                .id(200L)
                .user(customer)
                .amount(new BigDecimal("50000.00"))
                .interestRate(new BigDecimal("8.50"))
                .tenureMonths(12)
                .status(LoanStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should block an active account")
    void testBlockAccount() {
        when(accountRepository.findById(100L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        when(accountService.mapToResponse(any(Account.class))).thenReturn(
                AccountResponse.builder().id(100L).status(AccountStatus.BLOCKED).build()
        );

        AccountResponse response = adminService.blockAccount(100L);

        assertNotNull(response);
        assertEquals(AccountStatus.BLOCKED, account.getStatus());
    }

    @Test
    @DisplayName("Should unblock a blocked account")
    void testUnblockAccount() {
        account.setStatus(AccountStatus.BLOCKED);
        when(accountRepository.findById(100L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        when(accountService.mapToResponse(any(Account.class))).thenReturn(
                AccountResponse.builder().id(100L).status(AccountStatus.ACTIVE).build()
        );

        AccountResponse response = adminService.unblockAccount(100L);

        assertNotNull(response);
        assertEquals(AccountStatus.ACTIVE, account.getStatus());
    }

    @Test
    @DisplayName("Should approve a pending loan")
    void testApproveLoan() {
        when(loanRepository.findById(200L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);
        when(loanService.mapToResponse(any(Loan.class))).thenReturn(
                LoanResponse.builder().id(200L).status(LoanStatus.APPROVED).build()
        );

        LoanResponse response = adminService.approveLoan(200L, "Good credit score");

        assertNotNull(response);
        assertEquals(LoanStatus.APPROVED, loan.getStatus());
    }

    @Test
    @DisplayName("Should reject a pending loan")
    void testRejectLoan() {
        when(loanRepository.findById(200L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);
        when(loanService.mapToResponse(any(Loan.class))).thenReturn(
                LoanResponse.builder().id(200L).status(LoanStatus.REJECTED).build()
        );

        LoanResponse response = adminService.rejectLoan(200L, "High DTI ratio");

        assertNotNull(response);
        assertEquals(LoanStatus.REJECTED, loan.getStatus());
    }

    @Test
    @DisplayName("Should calculate system dashboard stats accurately")
    void testGetDashboardStats() {
        when(userRepository.count()).thenReturn(10L);
        when(accountRepository.findAll()).thenReturn(List.of(account));
        when(transactionRepository.count()).thenReturn(45L);
        when(loanRepository.countByStatus(LoanStatus.PENDING)).thenReturn(3L);
        when(loanRepository.countByStatus(LoanStatus.APPROVED)).thenReturn(5L);

        AdminDashboardStats stats = adminService.getDashboardStats();

        assertNotNull(stats);
        assertEquals(10L, stats.getTotalCustomers());
        assertEquals(1L, stats.getTotalAccounts());
        assertEquals(1L, stats.getActiveAccounts());
        assertEquals(0L, stats.getBlockedAccounts());
        assertEquals(new BigDecimal("15000.00"), stats.getTotalDepositVolume());
        assertEquals(45L, stats.getTotalTransactions());
        assertEquals(3L, stats.getPendingLoans());
        assertEquals(5L, stats.getApprovedLoans());
    }
}
