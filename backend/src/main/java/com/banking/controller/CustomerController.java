package com.banking.controller;

import com.banking.dto.AccountResponse;
import com.banking.dto.ApiResponse;
import com.banking.dto.ChangePasswordRequest;
import com.banking.dto.UpdateProfileRequest;
import com.banking.dto.UserProfileResponse;
import com.banking.service.CustomerService;
import com.banking.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "Endpoints for customer profile, password management and accounts")
@SecurityRequirement(name = "Bearer Authentication")
public class CustomerController {

    private final CustomerService customerService;
    private final SecurityUtils securityUtils;

    @GetMapping("/profile")
    @Operation(summary = "Get user profile", description = "Retrieves the profile of the currently logged in user")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile() {
        Long currentUserId = securityUtils.getCurrentUserId();
        UserProfileResponse profile = customerService.getProfile(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", profile));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update user profile", description = "Updates name and contact info for the current user")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        UserProfileResponse profile = customerService.updateProfile(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", profile));
    }

    @PutMapping("/change-password")
    @Operation(summary = "Change password", description = "Changes password after verifying old password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        customerService.changePassword(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    @GetMapping("/accounts")
    @Operation(summary = "Get customer accounts", description = "Retrieves all bank accounts owned by the current user")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAccounts() {
        Long currentUserId = securityUtils.getCurrentUserId();
        List<AccountResponse> accounts = customerService.getCustomerAccounts(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Accounts retrieved successfully", accounts));
    }
}
