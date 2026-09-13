package com.banking.service;

import com.banking.dto.BeneficiaryRequest;
import com.banking.dto.BeneficiaryResponse;
import com.banking.entity.Beneficiary;
import com.banking.entity.User;
import com.banking.enums.Role;
import com.banking.exception.DuplicateUserException;
import com.banking.exception.ResourceNotFoundException;
import com.banking.repository.BeneficiaryRepository;
import com.banking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BeneficiaryServiceTest {

    @Mock
    private BeneficiaryRepository beneficiaryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BeneficiaryService beneficiaryService;

    private User sampleUser;
    private Beneficiary sampleBeneficiary;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .role(Role.CUSTOMER)
                .build();

        sampleBeneficiary = Beneficiary.builder()
                .id(10L)
                .user(sampleUser)
                .beneficiaryName("Jane Smith")
                .accountNumber("200200300400")
                .bankName("CBII Bank")
                .ifscCode("CBII0001001")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should add beneficiary successfully")
    void testAddBeneficiarySuccess() {
        BeneficiaryRequest request = BeneficiaryRequest.builder()
                .beneficiaryName("Jane Smith")
                .accountNumber("200200300400")
                .bankName("CBII Bank")
                .ifscCode("CBII0001001")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(beneficiaryRepository.existsByUserIdAndAccountNumber(1L, "200200300400")).thenReturn(false);
        when(beneficiaryRepository.save(any(Beneficiary.class))).thenReturn(sampleBeneficiary);

        BeneficiaryResponse response = beneficiaryService.addBeneficiary(1L, request);

        assertNotNull(response);
        assertEquals("Jane Smith", response.getBeneficiaryName());
        assertEquals("200200300400", response.getAccountNumber());
        verify(beneficiaryRepository, times(1)).save(any(Beneficiary.class));
    }

    @Test
    @DisplayName("Should throw DuplicateUserException when beneficiary account is already added")
    void testAddDuplicateBeneficiary() {
        BeneficiaryRequest request = BeneficiaryRequest.builder()
                .beneficiaryName("Jane Smith")
                .accountNumber("200200300400")
                .bankName("CBII Bank")
                .ifscCode("CBII0001001")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(beneficiaryRepository.existsByUserIdAndAccountNumber(1L, "200200300400")).thenReturn(true);

        assertThrows(DuplicateUserException.class, () ->
                beneficiaryService.addBeneficiary(1L, request)
        );
        verify(beneficiaryRepository, never()).save(any(Beneficiary.class));
    }

    @Test
    @DisplayName("Should delete beneficiary if owned by user")
    void testDeleteBeneficiarySuccess() {
        when(beneficiaryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleBeneficiary));

        assertDoesNotThrow(() -> beneficiaryService.deleteBeneficiary(10L, 1L));
        verify(beneficiaryRepository, times(1)).delete(sampleBeneficiary);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException if beneficiary not found")
    void testDeleteBeneficiaryNotFound() {
        when(beneficiaryRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                beneficiaryService.deleteBeneficiary(99L, 1L)
        );
    }
}
