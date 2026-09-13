package com.banking.service;

import com.banking.dto.BeneficiaryRequest;
import com.banking.dto.BeneficiaryResponse;
import com.banking.entity.Beneficiary;
import com.banking.entity.User;
import com.banking.exception.DuplicateUserException;
import com.banking.exception.ResourceNotFoundException;
import com.banking.exception.UserNotFoundException;
import com.banking.repository.BeneficiaryRepository;
import com.banking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final UserRepository userRepository;

    @Transactional
    public BeneficiaryResponse addBeneficiary(Long userId, BeneficiaryRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if (beneficiaryRepository.existsByUserIdAndAccountNumber(userId, request.getAccountNumber().trim())) {
            throw new DuplicateUserException("Beneficiary with this account number is already added");
        }

        Beneficiary beneficiary = Beneficiary.builder()
                .user(user)
                .beneficiaryName(request.getBeneficiaryName().trim())
                .accountNumber(request.getAccountNumber().trim())
                .bankName(request.getBankName().trim())
                .ifscCode(request.getIfscCode().trim().toUpperCase())
                .build();

        Beneficiary saved = beneficiaryRepository.save(beneficiary);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> getBeneficiaries(Long userId) {
        return beneficiaryRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteBeneficiary(Long beneficiaryId, Long userId) {
        Beneficiary beneficiary = beneficiaryRepository.findByIdAndUserId(beneficiaryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary not found with id: " + beneficiaryId));

        beneficiaryRepository.delete(beneficiary);
    }

    private BeneficiaryResponse mapToResponse(Beneficiary b) {
        return BeneficiaryResponse.builder()
                .id(b.getId())
                .beneficiaryName(b.getBeneficiaryName())
                .accountNumber(b.getAccountNumber())
                .bankName(b.getBankName())
                .ifscCode(b.getIfscCode())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
