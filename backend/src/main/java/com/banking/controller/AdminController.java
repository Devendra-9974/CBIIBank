package com.banking.controller;

import com.banking.dto.*;
import com.banking.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'BANK_EMPLOYEE')")
@Tag(name = "Admin Operations", description = "Endpoints for administrators and bank staff to oversee users, accounts, transactions, and loans")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/customers")
    @Operation(summary = "Get all customers", description = "Returns a list of all registered users in the bank")
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> getAllCustomers() {
        List<UserProfileResponse> customers = adminService.getAllCustomers();
        return ResponseEntity.ok(ApiResponse.success("Customers retrieved successfully", customers));
    }

    @GetMapping("/loans")
    @Operation(summary = "Get all loans", description = "Returns all loan applications across the bank")
    public ResponseEntity<ApiResponse<List<LoanResponse>>> getAllLoans() {
        List<LoanResponse> loans = adminService.getAllLoans();
        return ResponseEntity.ok(ApiResponse.success("Loans retrieved successfully", loans));
    }

    @GetMapping("/accounts")
    @Operation(summary = "Get all accounts", description = "Returns all bank accounts across all users")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAllAccounts() {
        List<AccountResponse> accounts = adminService.getAllAccounts();
        return ResponseEntity.ok(ApiResponse.success("Accounts retrieved successfully", accounts));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get all system transactions", description = "Returns paginated list of all system transactions")
    public ResponseEntity<ApiResponse<PagedResponse<TransactionResponse>>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        PagedResponse<TransactionResponse> response = adminService.getAllTransactions(pageable);
        return ResponseEntity.ok(ApiResponse.success("System transactions retrieved successfully", response));
    }

    @PutMapping("/accounts/{id}/block")
    @Operation(summary = "Block bank account", description = "Blocks account from further debits/credits")
    public ResponseEntity<ApiResponse<AccountResponse>> blockAccount(@PathVariable Long id) {
        AccountResponse response = adminService.blockAccount(id);
        return ResponseEntity.ok(ApiResponse.success("Account blocked successfully", response));
    }

    @PutMapping("/accounts/{id}/unblock")
    @Operation(summary = "Unblock bank account", description = "Restores account to active status")
    public ResponseEntity<ApiResponse<AccountResponse>> unblockAccount(@PathVariable Long id) {
        AccountResponse response = adminService.unblockAccount(id);
        return ResponseEntity.ok(ApiResponse.success("Account unblocked successfully", response));
    }

    @PutMapping("/loans/{id}/approve")
    @Operation(summary = "Approve loan application", description = "Approves a pending loan")
    public ResponseEntity<ApiResponse<LoanResponse>> approveLoan(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks
    ) {
        LoanResponse response = adminService.approveLoan(id, remarks);
        return ResponseEntity.ok(ApiResponse.success("Loan approved successfully", response));
    }

    @PutMapping("/loans/{id}/reject")
    @Operation(summary = "Reject loan application", description = "Rejects a pending loan")
    public ResponseEntity<ApiResponse<LoanResponse>> rejectLoan(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks
    ) {
        LoanResponse response = adminService.rejectLoan(id, remarks);
        return ResponseEntity.ok(ApiResponse.success("Loan rejected", response));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get bank system dashboard metrics", description = "Calculates total counts, balances, and loan metrics")
    public ResponseEntity<ApiResponse<AdminDashboardStats>> getDashboardStats() {
        AdminDashboardStats stats = adminService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success("Dashboard statistics retrieved successfully", stats));
    }
}
