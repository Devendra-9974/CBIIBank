package com.banking.dto;

import com.banking.enums.TransactionStatus;
import com.banking.enums.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    private Long id;
    private String transactionReference;
    private String sourceAccountNumber;
    private String targetAccountNumber;
    private String sourceUserName;
    private String targetUserName;
    private TransactionType type;
    private BigDecimal amount;
    private TransactionStatus status;
    private String description;
    private LocalDateTime timestamp;
}
