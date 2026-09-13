package com.banking.controller;

import com.banking.dto.AccountResponse;
import com.banking.dto.ApiResponse;
import com.banking.dto.BalanceResponse;
import com.banking.dto.CreateAccountRequest;
import com.banking.enums.Role;
import com.banking.security.UserDetailsImpl;
import com.banking.service.AccountService;
import com.banking.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "Bank Accounts", description = "Endpoints for bank account creation, balance inquiry, and closure")
@SecurityRequirement(name = "Bearer Authentication")
public class AccountController {

    private final AccountService accountService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @Operation(summary = "Create a new bank account", description = "Opens a savings or checking account for current user")
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        AccountResponse response = accountService.createAccount(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bank account created successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account details by ID", description = "Retrieves account information if owned by user or if admin")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountById(@PathVariable Long id) {
        UserDetailsImpl userDetails = securityUtils.getCurrentUserDetails();
        Role userRole = Role.valueOf(userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""));
        AccountResponse response = accountService.getAccountById(id, userDetails.getId(), userRole);
        return ResponseEntity.ok(ApiResponse.success("Account details retrieved successfully", response));
    }

    @GetMapping("/{id}/balance")
    @Operation(summary = "Get account balance", description = "Retrieves current balance of specified account")
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalance(@PathVariable Long id) {
        UserDetailsImpl userDetails = securityUtils.getCurrentUserDetails();
        Role userRole = Role.valueOf(userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""));
        BalanceResponse response = accountService.getAccountBalance(id, userDetails.getId(), userRole);
        return ResponseEntity.ok(ApiResponse.success("Balance retrieved successfully", response));
    }

    @PutMapping("/{id}/close")
    @Operation(summary = "Close bank account", description = "Closes account if balance is 0.00")
    public ResponseEntity<ApiResponse<AccountResponse>> closeAccount(@PathVariable Long id) {
        UserDetailsImpl userDetails = securityUtils.getCurrentUserDetails();
        Role userRole = Role.valueOf(userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""));
        AccountResponse response = accountService.closeAccount(id, userDetails.getId(), userRole);
        return ResponseEntity.ok(ApiResponse.success("Account closed successfully", response));
    }
}
