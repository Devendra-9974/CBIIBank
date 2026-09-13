package com.banking.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardStats {
    private long totalCustomers;
    private long totalAccounts;
    private long totalTransactions;
    private BigDecimal totalDepositVolume;
    private long pendingLoans;
    private long approvedLoans;
    private long activeAccounts;
    private long blockedAccounts;
}
