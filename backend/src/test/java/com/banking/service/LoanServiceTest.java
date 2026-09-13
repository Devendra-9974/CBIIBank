package com.banking.service;

import com.banking.dto.LoanApplicationRequest;
import com.banking.dto.LoanResponse;
import com.banking.dto.LoanStatusUpdateRequest;
import com.banking.entity.Loan;
import com.banking.entity.User;
import com.banking.enums.LoanStatus;
import com.banking.enums.Role;
import com.banking.exception.InvalidTransactionException;
import com.banking.repository.LoanRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LoanService loanService;

    private User sampleUser;
    private Loan sampleLoan;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .role(Role.CUSTOMER)
                .build();

        sampleLoan = Loan.builder()
                .id(201L)
                .user(sampleUser)
                .amount(new BigDecimal("100000.00"))
                .interestRate(new BigDecimal("9.25"))
                .tenureMonths(24)
                .status(LoanStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should apply for loan with calculated interest rate")
    void testApplyLoan() {
        LoanApplicationRequest request = LoanApplicationRequest.builder()
                .amount(new BigDecimal("40000.00"))
                .tenureMonths(12)
                .remarks("Personal loan")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(loanRepository.save(any(Loan.class))).thenAnswer(i -> {
            Loan l = i.getArgument(0);
            l.setId(202L);
            l.setCreatedAt(LocalDateTime.now());
            return l;
        });

        LoanResponse response = loanService.applyLoan(1L, request);

        assertNotNull(response);
        assertEquals(new BigDecimal("40000.00"), response.getAmount());
        assertEquals(new BigDecimal("8.50"), response.getInterestRate());
        assertEquals(LoanStatus.PENDING, response.getStatus());
    }

    @Test
    @DisplayName("Should approve a pending loan")
    void testApproveLoan() {
        LoanStatusUpdateRequest request = LoanStatusUpdateRequest.builder()
                .status(LoanStatus.APPROVED)
                .remarks("Approved by Branch Manager")
                .build();

        when(loanRepository.findById(201L)).thenReturn(Optional.of(sampleLoan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(i -> i.getArgument(0));

        LoanResponse response = loanService.updateLoanStatus(201L, request);

        assertNotNull(response);
        assertEquals(LoanStatus.APPROVED, response.getStatus());
        assertEquals("Approved by Branch Manager", response.getRemarks());
    }

    @Test
    @DisplayName("Should reject updating an already approved or rejected loan")
    void testUpdateAlreadyProcessedLoanFails() {
        sampleLoan.setStatus(LoanStatus.APPROVED);

        LoanStatusUpdateRequest request = LoanStatusUpdateRequest.builder()
                .status(LoanStatus.REJECTED)
                .build();

        when(loanRepository.findById(201L)).thenReturn(Optional.of(sampleLoan));

        assertThrows(InvalidTransactionException.class, () ->
                loanService.updateLoanStatus(201L, request)
        );
    }
}
