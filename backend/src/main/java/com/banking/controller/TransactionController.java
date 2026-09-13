package com.banking.controller;

import com.banking.dto.ApiResponse;
import com.banking.dto.DepositRequest;
import com.banking.dto.PagedResponse;
import com.banking.dto.TransactionResponse;
import com.banking.dto.TransferRequest;
import com.banking.dto.WithdrawRequest;
import com.banking.enums.Role;
import com.banking.security.UserDetailsImpl;
import com.banking.service.TransactionService;
import com.banking.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Endpoints for deposit, withdrawal, atomic fund transfer, and transaction history")
@SecurityRequirement(name = "Bearer Authentication")
public class TransactionController {

    private final TransactionService transactionService;
    private final SecurityUtils securityUtils;

    @PostMapping("/deposit")
    @Operation(summary = "Deposit funds into account", description = "Deposits money into an active bank account")
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(@Valid @RequestBody DepositRequest request) {
        UserDetailsImpl userDetails = securityUtils.getCurrentUserDetails();
        Role userRole = Role.valueOf(userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""));
        TransactionResponse response = transactionService.deposit(request, userDetails.getId(), userRole);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Deposit completed successfully", response));
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw funds from account", description = "Withdraws money if account has sufficient balance")
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(@Valid @RequestBody WithdrawRequest request) {
        UserDetailsImpl userDetails = securityUtils.getCurrentUserDetails();
        Role userRole = Role.valueOf(userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""));
        TransactionResponse response = transactionService.withdraw(request, userDetails.getId(), userRole);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Withdrawal completed successfully", response));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer money between accounts", description = "Atomically transfers funds from source to destination account")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(@Valid @RequestBody TransferRequest request) {
        UserDetailsImpl userDetails = securityUtils.getCurrentUserDetails();
        Role userRole = Role.valueOf(userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""));
        TransactionResponse response = transactionService.transfer(request, userDetails.getId(), userRole);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transfer completed successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get transaction history", description = "Returns user transactions (or all if admin) with optional pagination")
    public ResponseEntity<ApiResponse<Object>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "true") boolean paged
    ) {
        UserDetailsImpl userDetails = securityUtils.getCurrentUserDetails();
        Role userRole = Role.valueOf(userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""));

        if (!paged) {
            List<TransactionResponse> list = transactionService.getTransactionsForUser(userDetails.getId(), userRole);
            return ResponseEntity.ok(ApiResponse.success("Transactions retrieved successfully", list));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        PagedResponse<TransactionResponse> pagedResponse = transactionService.getTransactionsForUserPaged(userDetails.getId(), userRole, pageable);
        return ResponseEntity.ok(ApiResponse.success("Transactions retrieved successfully", pagedResponse));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction details by ID", description = "Retrieves specific transaction details")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionById(@PathVariable Long id) {
        UserDetailsImpl userDetails = securityUtils.getCurrentUserDetails();
        Role userRole = Role.valueOf(userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""));
        TransactionResponse response = transactionService.getTransactionById(id, userDetails.getId(), userRole);
        return ResponseEntity.ok(ApiResponse.success("Transaction retrieved successfully", response));
    }
}
