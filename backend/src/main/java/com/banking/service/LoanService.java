package com.banking.service;

import com.banking.dto.LoanApplicationRequest;
import com.banking.dto.LoanResponse;
import com.banking.dto.LoanStatusUpdateRequest;
import com.banking.entity.Loan;
import com.banking.entity.User;
import com.banking.enums.LoanStatus;
import com.banking.enums.Role;
import com.banking.exception.InvalidTransactionException;
import com.banking.exception.LoanNotFoundException;
import com.banking.exception.UnauthorizedException;
import com.banking.exception.UserNotFoundException;
import com.banking.repository.LoanRepository;
import com.banking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;

    @Transactional
    public LoanResponse applyLoan(Long userId, LoanApplicationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        // Determine interest rate based on loan amount/tenure
        BigDecimal interestRate;
        if (request.getAmount().compareTo(new BigDecimal("50000.00")) <= 0) {
            interestRate = new BigDecimal("8.50");
        } else if (request.getAmount().compareTo(new BigDecimal("500000.00")) <= 0) {
            interestRate = new BigDecimal("9.25");
        } else {
            interestRate = new BigDecimal("10.50");
        }

        Loan loan = Loan.builder()
                .user(user)
                .amount(request.getAmount())
                .interestRate(interestRate)
                .tenureMonths(request.getTenureMonths())
                .status(LoanStatus.PENDING)
                .remarks(request.getRemarks() != null ? request.getRemarks() : "Personal/Retail Loan Application")
                .build();

        Loan saved = loanRepository.save(loan);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> getLoansForUser(Long userId) {
        return loanRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LoanResponse getLoanById(Long loanId, Long currentUserId, Role currentRole) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new LoanNotFoundException("Loan application not found with id: " + loanId));

        if (currentRole == Role.CUSTOMER && !loan.getUser().getId().equals(currentUserId)) {
            throw new UnauthorizedException("You are not authorized to view this loan application");
        }

        return mapToResponse(loan);
    }

    @Transactional
    public LoanResponse updateLoanStatus(Long loanId, LoanStatusUpdateRequest request) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new LoanNotFoundException("Loan application not found with id: " + loanId));

        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new InvalidTransactionException("Loan has already been processed with status: " + loan.getStatus());
        }

        loan.setStatus(request.getStatus());
        if (request.getRemarks() != null && !request.getRemarks().isBlank()) {
            loan.setRemarks(request.getRemarks());
        }

        Loan saved = loanRepository.save(loan);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> getAllLoans() {
        return loanRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public LoanResponse mapToResponse(Loan loan) {
        return LoanResponse.builder()
                .id(loan.getId())
                .userId(loan.getUser().getId())
                .userName(loan.getUser().getName())
                .userEmail(loan.getUser().getEmail())
                .amount(loan.getAmount())
                .interestRate(loan.getInterestRate())
                .tenureMonths(loan.getTenureMonths())
                .status(loan.getStatus())
                .remarks(loan.getRemarks())
                .createdAt(loan.getCreatedAt())
                .updatedAt(loan.getUpdatedAt())
                .build();
    }
}
