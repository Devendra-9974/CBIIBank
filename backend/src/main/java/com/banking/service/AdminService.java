package com.banking.service;

import com.banking.dto.*;
import com.banking.entity.Account;
import com.banking.entity.Loan;
import com.banking.entity.User;
import com.banking.enums.AccountStatus;
import com.banking.enums.LoanStatus;
import com.banking.enums.Role;
import com.banking.exception.AccountNotFoundException;
import com.banking.exception.InvalidTransactionException;
import com.banking.exception.LoanNotFoundException;
import com.banking.repository.AccountRepository;
import com.banking.repository.LoanRepository;
import com.banking.repository.TransactionRepository;
import com.banking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LoanRepository loanRepository;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final LoanService loanService;

    @Transactional(readOnly = true)
    public List<LoanResponse> getAllLoans() {
        return loanService.getAllLoans();
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> getAllCustomers() {
        return userRepository.findAll().stream()
                .map(user -> UserProfileResponse.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .role(user.getRole())
                        .createdAt(user.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(accountService::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PagedResponse<TransactionResponse> getAllTransactions(Pageable pageable) {
        Page<com.banking.entity.Transaction> page = transactionRepository.findAllByOrderByTimestampDesc(pageable);
        List<TransactionResponse> content = page.getContent().stream()
                .map(transactionService::mapToResponse)
                .collect(Collectors.toList());

        return PagedResponse.<TransactionResponse>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .build();
    }

    @Transactional
    public AccountResponse blockAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + accountId));

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new InvalidTransactionException("Cannot block a closed account");
        }

        account.setStatus(AccountStatus.BLOCKED);
        Account saved = accountRepository.save(account);
        return accountService.mapToResponse(saved);
    }

    @Transactional
    public AccountResponse unblockAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + accountId));

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new InvalidTransactionException("Cannot unblock a closed account");
        }

        account.setStatus(AccountStatus.ACTIVE);
        Account saved = accountRepository.save(account);
        return accountService.mapToResponse(saved);
    }

    @Transactional
    public LoanResponse approveLoan(Long loanId, String remarks) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new LoanNotFoundException("Loan not found with id: " + loanId));

        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new InvalidTransactionException("Loan has already been processed with status: " + loan.getStatus());
        }

        loan.setStatus(LoanStatus.APPROVED);
        if (remarks != null && !remarks.isBlank()) {
            loan.setRemarks(remarks);
        } else {
            loan.setRemarks("Loan approved by administrative authority");
        }

        Loan saved = loanRepository.save(loan);
        return loanService.mapToResponse(saved);
    }

    @Transactional
    public LoanResponse rejectLoan(Long loanId, String remarks) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new LoanNotFoundException("Loan not found with id: " + loanId));

        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new InvalidTransactionException("Loan has already been processed with status: " + loan.getStatus());
        }

        loan.setStatus(LoanStatus.REJECTED);
        if (remarks != null && !remarks.isBlank()) {
            loan.setRemarks(remarks);
        } else {
            loan.setRemarks("Loan application rejected due to credit assessment criteria");
        }

        Loan saved = loanRepository.save(loan);
        return loanService.mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public AdminDashboardStats getDashboardStats() {
        long totalCustomers = userRepository.count();
        List<Account> accounts = accountRepository.findAll();
        long totalAccounts = accounts.size();
        long activeAccounts = accounts.stream().filter(a -> a.getStatus() == AccountStatus.ACTIVE).count();
        long blockedAccounts = accounts.stream().filter(a -> a.getStatus() == AccountStatus.BLOCKED).count();

        BigDecimal totalDepositVolume = accounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalTransactions = transactionRepository.count();
        long pendingLoans = loanRepository.countByStatus(LoanStatus.PENDING);
        long approvedLoans = loanRepository.countByStatus(LoanStatus.APPROVED);

        return AdminDashboardStats.builder()
                .totalCustomers(totalCustomers)
                .totalAccounts(totalAccounts)
                .activeAccounts(activeAccounts)
                .blockedAccounts(blockedAccounts)
                .totalDepositVolume(totalDepositVolume)
                .totalTransactions(totalTransactions)
                .pendingLoans(pendingLoans)
                .approvedLoans(approvedLoans)
                .build();
    }
}
