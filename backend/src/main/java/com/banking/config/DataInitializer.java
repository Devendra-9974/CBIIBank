package com.banking.config;

import com.banking.entity.*;
import com.banking.enums.*;
import com.banking.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final LoanRepository loanRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            logger.info("Database already initialized. Skipping seed data.");
            return;
        }

        logger.info("Initializing CBII Bank database with Indian demo users, accounts, and transactions...");

        // 1. Admin & Primary NetBanking User (devkmishra402@gmail.com / DevAnil@789974)
        User admin = User.builder()
                .name("Devendra Kumar Mishra")
                .email("devkmishra402@gmail.com")
                .password(passwordEncoder.encode("DevAnil@789974"))
                .phone("9876543210")
                .role(Role.ADMIN)
                .build();
        userRepository.save(admin);

        Account devSavings = Account.builder()
                .accountNumber("100100200300")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("150000.00"))
                .status(AccountStatus.ACTIVE)
                .user(admin)
                .build();
        accountRepository.save(devSavings);

        Account devCurrent = Account.builder()
                .accountNumber("100100200301")
                .accountType(AccountType.CHECKING)
                .balance(new BigDecimal("75000.00"))
                .status(AccountStatus.ACTIVE)
                .user(admin)
                .build();
        accountRepository.save(devCurrent);

        // 2. Bank Employee / Branch Officer
        User employee = User.builder()
                .name("Priya Sharma")
                .email("employee@cbiibank.com")
                .password(passwordEncoder.encode("DevAnil@789974"))
                .phone("9876543211")
                .role(Role.BANK_EMPLOYEE)
                .build();
        userRepository.save(employee);

        // 3. Indian Customer 1 - Aarav Patel
        User customer1 = User.builder()
                .name("Aarav Patel")
                .email("aarav.patel@gmail.com")
                .password(passwordEncoder.encode("Customer@123"))
                .phone("9876543212")
                .role(Role.CUSTOMER)
                .build();
        userRepository.save(customer1);

        Account aaravSavings = Account.builder()
                .accountNumber("200200300400")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("85000.00"))
                .status(AccountStatus.ACTIVE)
                .user(customer1)
                .build();
        accountRepository.save(aaravSavings);

        // 4. Indian Customer 2 - Ananya Verma
        User customer2 = User.builder()
                .name("Ananya Verma")
                .email("ananya.verma@gmail.com")
                .password(passwordEncoder.encode("Customer@123"))
                .phone("9876543213")
                .role(Role.CUSTOMER)
                .build();
        userRepository.save(customer2);

        Account ananyaSavings = Account.builder()
                .accountNumber("300300400500")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("45000.00"))
                .status(AccountStatus.ACTIVE)
                .user(customer2)
                .build();
        accountRepository.save(ananyaSavings);

        // 5. Seed initial transactions
        Transaction tx1 = Transaction.builder()
                .transactionReference("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .sourceAccount(null)
                .targetAccount(devSavings)
                .type(TransactionType.DEPOSIT)
                .amount(new BigDecimal("150000.00"))
                .status(TransactionStatus.SUCCESS)
                .description("Initial Opening Deposit - NEFT/UPI")
                .build();
        transactionRepository.save(tx1);

        Transaction tx2 = Transaction.builder()
                .transactionReference("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .sourceAccount(devSavings)
                .targetAccount(aaravSavings)
                .type(TransactionType.TRANSFER)
                .amount(new BigDecimal("15000.00"))
                .status(TransactionStatus.SUCCESS)
                .description("Vendor Payment - IMPS Settlement")
                .build();
        transactionRepository.save(tx2);

        // 6. Seed Beneficiaries for Devendra
        Beneficiary ben1 = Beneficiary.builder()
                .user(admin)
                .beneficiaryName("Aarav Patel")
                .accountNumber("200200300400")
                .bankName("CBII Bank")
                .ifscCode("CBII0001001")
                .build();
        beneficiaryRepository.save(ben1);

        Beneficiary ben2 = Beneficiary.builder()
                .user(admin)
                .beneficiaryName("Ananya Verma")
                .accountNumber("300300400500")
                .bankName("State Bank of India")
                .ifscCode("SBIN0000300")
                .build();
        beneficiaryRepository.save(ben2);

        // 7. Seed Sample Loan for Devendra
        Loan loan1 = Loan.builder()
                .user(admin)
                .amount(new BigDecimal("500000.00"))
                .interestRate(new BigDecimal("8.50"))
                .tenureMonths(36)
                .status(LoanStatus.PENDING)
                .remarks("Commercial Business Expansion Loan")
                .build();
        loanRepository.save(loan1);

        logger.info("CBII Bank demo data loaded successfully!");
        logger.info("Admin/Primary Login -> devkmishra402@gmail.com / DevAnil@789974");
        logger.info("Employee Login -> employee@cbiibank.com / DevAnil@789974");
        logger.info("Customer Logins -> aarav.patel@gmail.com / Customer@123 | ananya.verma@gmail.com / Customer@123");
    }
}
