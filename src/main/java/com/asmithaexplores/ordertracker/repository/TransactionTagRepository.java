package com.asmithaexplores.ordertracker.repository;

import com.asmithaexplores.ordertracker.entity.TransactionTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionTagRepository extends JpaRepository<TransactionTag, Long> {

    List<TransactionTag> findAllByOrderByNameAsc();

    Optional<TransactionTag> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
