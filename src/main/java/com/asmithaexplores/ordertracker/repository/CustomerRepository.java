package com.asmithaexplores.ordertracker.repository;

import com.asmithaexplores.ordertracker.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByMobileNumber(String mobileNumber);

    boolean existsByMobileNumber(String mobileNumber);

    List<Customer> findAllByOrderByNameAsc();

    @Query("""
            select c from Customer c
            where lower(c.name) like lower(concat('%', :term, '%'))
               or c.mobileNumber like concat('%', :term, '%')
               or c.alternateMobileNumber like concat('%', :term, '%')
            order by c.name asc
            """)
    List<Customer> search(@Param("term") String term);
}

