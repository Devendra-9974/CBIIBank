package com.banking.controller;

import com.banking.dto.ApiResponse;
import com.banking.dto.LoanApplicationRequest;
import com.banking.dto.LoanResponse;
import com.banking.dto.LoanStatusUpdateRequest;
import com.banking.enums.Role;
import com.banking.security.UserDetailsImpl;
import com.banking.service.LoanService;
import com.banking.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
@Tag(name = "Loans", description = "Endpoints for loan application, status tracking, and loan approvals")
@SecurityRequirement(name = "Bearer Authentication")
public class LoanController {

    private final LoanService loanService;
    private final SecurityUtils securityUtils;

    @PostMapping("/apply")
    @Operation(summary = "Apply for a new loan", description = "Submits a new loan request for the current customer")
    public ResponseEntity<ApiResponse<LoanResponse>> applyLoan(@Valid @RequestBody LoanApplicationRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        LoanResponse response = loanService.applyLoan(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Loan application submitted successfully", response));
    }

    @GetMapping("/my-loans")
    @Operation(summary = "Get user loans", description = "Lists all loan applications submitted by the current user")
    public ResponseEntity<ApiResponse<List<LoanResponse>>> getMyLoans() {
        Long currentUserId = securityUtils.getCurrentUserId();
        List<LoanResponse> list = loanService.getLoansForUser(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Loans retrieved successfully", list));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get loan details by ID", description = "Retrieves specific loan details")
    public ResponseEntity<ApiResponse<LoanResponse>> getLoanById(@PathVariable Long id) {
        UserDetailsImpl userDetails = securityUtils.getCurrentUserDetails();
        Role userRole = Role.valueOf(userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""));
        LoanResponse response = loanService.getLoanById(id, userDetails.getId(), userRole);
        return ResponseEntity.ok(ApiResponse.success("Loan details retrieved successfully", response));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'BANK_EMPLOYEE')")
    @Operation(summary = "Update loan status (Admin / Employee)", description = "Approves or rejects a pending loan")
    public ResponseEntity<ApiResponse<LoanResponse>> updateLoanStatus(
            @PathVariable Long id,
            @Valid @RequestBody LoanStatusUpdateRequest request
    ) {
        LoanResponse response = loanService.updateLoanStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Loan status updated to " + request.getStatus(), response));
    }
}
