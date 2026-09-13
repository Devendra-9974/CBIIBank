package com.banking.repository;

import com.banking.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionReference(String transactionReference);

    @Query("SELECT t FROM Transaction t WHERE (t.sourceAccount.id IN :accountIds OR t.targetAccount.id IN :accountIds) ORDER BY t.timestamp DESC")
    List<Transaction> findByAccountIds(@Param("accountIds") List<Long> accountIds);

    @Query("SELECT t FROM Transaction t WHERE (t.sourceAccount.id IN :accountIds OR t.targetAccount.id IN :accountIds) ORDER BY t.timestamp DESC")
    Page<Transaction> findByAccountIdsPaged(@Param("accountIds") List<Long> accountIds, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE (t.sourceAccount.id = :accountId OR t.targetAccount.id = :accountId) ORDER BY t.timestamp DESC")
    List<Transaction> findByAccountId(@Param("accountId") Long accountId);

    Page<Transaction> findAllByOrderByTimestampDesc(Pageable pageable);
}
