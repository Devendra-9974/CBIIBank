package com.banking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "beneficiaries", indexes = {
    @Index(name = "idx_beneficiary_user", columnList = "user_id"),
    @Index(name = "idx_beneficiary_acc", columnList = "accountNumber")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String beneficiaryName;

    @Column(nullable = false, length = 20)
    private String accountNumber;

    @Column(nullable = false, length = 100)
    private String bankName;

    @Column(nullable = false, length = 20)
    private String ifscCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
