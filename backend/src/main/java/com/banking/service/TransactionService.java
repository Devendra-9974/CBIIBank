package com.banking.service;

import com.banking.dto.DepositRequest;
import com.banking.dto.PagedResponse;
import com.banking.dto.TransactionResponse;
import com.banking.dto.TransferRequest;
import com.banking.dto.WithdrawRequest;
import com.banking.entity.Account;
import com.banking.entity.Transaction;
import com.banking.enums.AccountStatus;
import com.banking.enums.Role;
import com.banking.enums.TransactionStatus;
import com.banking.enums.TransactionType;
import com.banking.exception.AccountNotFoundException;
import com.banking.exception.InsufficientBalanceException;
import com.banking.exception.InvalidTransactionException;
import com.banking.exception.ResourceNotFoundException;
import com.banking.exception.UnauthorizedException;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse deposit(DepositRequest request, Long currentUserId, Role currentRole) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Deposit amount must be greater than zero");
        }

        Account account = accountRepository.findByAccountNumberWithLock(request.getAccountNumber().trim())
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + request.getAccountNumber()));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidTransactionException("Cannot deposit to an inactive or blocked account. Status: " + account.getStatus());
        }

        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .transactionReference("DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .sourceAccount(null)
                .targetAccount(account)
                .type(TransactionType.DEPOSIT)
                .amount(request.getAmount())
                .status(TransactionStatus.SUCCESS)
                .description(request.getDescription() != null ? request.getDescription() : "Cash/Online Deposit")
                .build();

        Transaction savedTx = transactionRepository.save(transaction);
        return mapToResponse(savedTx);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse withdraw(WithdrawRequest request, Long currentUserId, Role currentRole) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Withdrawal amount must be greater than zero");
        }

        Account account = accountRepository.findByAccountNumberWithLock(request.getAccountNumber().trim())
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + request.getAccountNumber()));

        if (currentRole == Role.CUSTOMER && !account.getUser().getId().equals(currentUserId)) {
            throw new UnauthorizedException("You are not authorized to withdraw from this account");
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidTransactionException("Cannot withdraw from an inactive or blocked account. Status: " + account.getStatus());
        }

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient funds. Available balance: " + account.getBalance() + ", Requested: " + request.getAmount());
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .transactionReference("WTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .sourceAccount(account)
                .targetAccount(null)
                .type(TransactionType.WITHDRAWAL)
                .amount(request.getAmount())
                .status(TransactionStatus.SUCCESS)
                .description(request.getDescription() != null ? request.getDescription() : "ATM/Online Withdrawal")
                .build();

        Transaction savedTx = transactionRepository.save(transaction);
        return mapToResponse(savedTx);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public TransactionResponse transfer(TransferRequest request, Long currentUserId, Role currentRole) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transfer amount must be greater than zero");
        }

        String sourceAccNum = request.getSourceAccountNumber().trim();
        String targetAccNum = request.getTargetAccountNumber().trim();

        if (sourceAccNum.equalsIgnoreCase(targetAccNum)) {
            throw new InvalidTransactionException("Source and destination accounts cannot be the same");
        }

        // Deadlock prevention: Lock accounts in a deterministic order (lexicographically by account number)
        Account firstLock;
        Account secondLock;
        if (sourceAccNum.compareTo(targetAccNum) < 0) {
            firstLock = accountRepository.findByAccountNumberWithLock(sourceAccNum)
                    .orElseThrow(() -> new AccountNotFoundException("Source account not found: " + sourceAccNum));
            secondLock = accountRepository.findByAccountNumberWithLock(targetAccNum)
                    .orElseThrow(() -> new AccountNotFoundException("Destination account not found: " + targetAccNum));
        } else {
            secondLock = accountRepository.findByAccountNumberWithLock(targetAccNum)
                    .orElseThrow(() -> new AccountNotFoundException("Destination account not found: " + targetAccNum));
            firstLock = accountRepository.findByAccountNumberWithLock(sourceAccNum)
                    .orElseThrow(() -> new AccountNotFoundException("Source account not found: " + sourceAccNum));
        }

        Account sourceAccount = sourceAccNum.equals(firstLock.getAccountNumber()) ? firstLock : secondLock;
        Account targetAccount = targetAccNum.equals(firstLock.getAccountNumber()) ? firstLock : secondLock;

        // Authorization check
        if (currentRole == Role.CUSTOMER && !sourceAccount.getUser().getId().equals(currentUserId)) {
            throw new UnauthorizedException("You are not authorized to transfer from this account");
        }

        // Status validation
        if (sourceAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidTransactionException("Source account is " + sourceAccount.getStatus() + ". Transfer not allowed.");
        }
        if (targetAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidTransactionException("Destination account is " + targetAccount.getStatus() + ". Transfer not allowed.");
        }

        // Balance validation
        if (sourceAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance in source account. Available: " + sourceAccount.getBalance() + ", Requested: " + request.getAmount());
        }

        // Atomic Deduct & Credit
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(request.getAmount()));
        targetAccount.setBalance(targetAccount.getBalance().add(request.getAmount()));

        accountRepository.save(sourceAccount);
        accountRepository.save(targetAccount);

        Transaction transaction = Transaction.builder()
                .transactionReference("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .sourceAccount(sourceAccount)
                .targetAccount(targetAccount)
                .type(TransactionType.TRANSFER)
                .amount(request.getAmount())
                .status(TransactionStatus.SUCCESS)
                .description(request.getDescription() != null && !request.getDescription().isBlank() ? request.getDescription() : "Fund Transfer")
                .build();

        Transaction savedTx = transactionRepository.save(transaction);
        return mapToResponse(savedTx);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsForUser(Long currentUserId, Role currentRole) {
        if (currentRole == Role.ADMIN || currentRole == Role.BANK_EMPLOYEE) {
            return transactionRepository.findAll().stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        List<Account> userAccounts = accountRepository.findByUserId(currentUserId);
        List<Long> accountIds = userAccounts.stream().map(Account::getId).collect(Collectors.toList());

        if (accountIds.isEmpty()) {
            return List.of();
        }

        return transactionRepository.findByAccountIds(accountIds).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PagedResponse<TransactionResponse> getTransactionsForUserPaged(Long currentUserId, Role currentRole, Pageable pageable) {
        Page<Transaction> page;
        if (currentRole == Role.ADMIN || currentRole == Role.BANK_EMPLOYEE) {
            page = transactionRepository.findAllByOrderByTimestampDesc(pageable);
        } else {
            List<Account> userAccounts = accountRepository.findByUserId(currentUserId);
            List<Long> accountIds = userAccounts.stream().map(Account::getId).collect(Collectors.toList());
            if (accountIds.isEmpty()) {
                return PagedResponse.<TransactionResponse>builder()
                        .content(List.of())
                        .pageNumber(pageable.getPageNumber())
                        .pageSize(pageable.getPageSize())
                        .totalElements(0)
                        .totalPages(0)
                        .isLast(true)
                        .build();
            }
            page = transactionRepository.findByAccountIdsPaged(accountIds, pageable);
        }

        List<TransactionResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
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

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(Long transactionId, Long currentUserId, Role currentRole) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        if (currentRole == Role.CUSTOMER) {
            boolean isSourceOwner = tx.getSourceAccount() != null && tx.getSourceAccount().getUser().getId().equals(currentUserId);
            boolean isTargetOwner = tx.getTargetAccount() != null && tx.getTargetAccount().getUser().getId().equals(currentUserId);
            if (!isSourceOwner && !isTargetOwner) {
                throw new UnauthorizedException("You are not authorized to view this transaction");
            }
        }

        return mapToResponse(tx);
    }

    public TransactionResponse mapToResponse(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .transactionReference(tx.getTransactionReference())
                .sourceAccountNumber(tx.getSourceAccount() != null ? tx.getSourceAccount().getAccountNumber() : "N/A")
                .targetAccountNumber(tx.getTargetAccount() != null ? tx.getTargetAccount().getAccountNumber() : "N/A")
                .sourceUserName(tx.getSourceAccount() != null ? tx.getSourceAccount().getUser().getName() : "Bank External/Cash")
                .targetUserName(tx.getTargetAccount() != null ? tx.getTargetAccount().getUser().getName() : "Bank External/Cash")
                .type(tx.getType())
                .amount(tx.getAmount())
                .status(tx.getStatus())
                .description(tx.getDescription())
                .timestamp(tx.getTimestamp())
                .build();
    }
}
