package com.banking.controller;

import com.banking.dto.ApiResponse;
import com.banking.dto.BeneficiaryRequest;
import com.banking.dto.BeneficiaryResponse;
import com.banking.service.BeneficiaryService;
import com.banking.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/beneficiaries")
@RequiredArgsConstructor
@Tag(name = "Beneficiaries", description = "Endpoints for managing transfer beneficiaries / payees")
@SecurityRequirement(name = "Bearer Authentication")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @Operation(summary = "Add a new beneficiary", description = "Saves beneficiary details for future transfers")
    public ResponseEntity<ApiResponse<BeneficiaryResponse>> addBeneficiary(@Valid @RequestBody BeneficiaryRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        BeneficiaryResponse response = beneficiaryService.addBeneficiary(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Beneficiary added successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get user beneficiaries", description = "Lists all saved beneficiaries for the authenticated user")
    public ResponseEntity<ApiResponse<List<BeneficiaryResponse>>> getBeneficiaries() {
        Long currentUserId = securityUtils.getCurrentUserId();
        List<BeneficiaryResponse> list = beneficiaryService.getBeneficiaries(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Beneficiaries retrieved successfully", list));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete beneficiary", description = "Removes a beneficiary by ID")
    public ResponseEntity<ApiResponse<Void>> deleteBeneficiary(@PathVariable Long id) {
        Long currentUserId = securityUtils.getCurrentUserId();
        beneficiaryService.deleteBeneficiary(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Beneficiary deleted successfully"));
    }
}
