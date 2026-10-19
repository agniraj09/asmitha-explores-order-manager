package com.asmithaexplores.ordertracker.service;

import com.asmithaexplores.ordertracker.dto.CustomerForm;
import com.asmithaexplores.ordertracker.dto.CustomerView;
import com.asmithaexplores.ordertracker.entity.Customer;
import com.asmithaexplores.ordertracker.exception.BusinessRuleException;
import com.asmithaexplores.ordertracker.exception.ResourceNotFoundException;
import com.asmithaexplores.ordertracker.repository.CustomerRepository;
import com.asmithaexplores.ordertracker.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    public List<CustomerView> findAll(String search) {
        List<Customer> customers = StringUtils.hasText(search)
                ? customerRepository.search(search.trim())
                : customerRepository.findAllByOrderByNameAsc();
        Map<Long, Long> counts = orderCountsByCustomer();
        return customers.stream()
                .map(c -> toView(c, counts.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    public List<Customer> findAllCustomers() {
        return customerRepository.findAllByOrderByNameAsc();
    }

    public Customer getEntity(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", id));
    }

    public CustomerView getView(Long id) {
        Customer customer = getEntity(id);
        return toView(customer, orderRepository.countByCustomerId(id));
    }

    public CustomerForm getForm(Long id) {
        Customer customer = getEntity(id);
        return CustomerForm.builder()
                .id(customer.getId())
                .name(customer.getName())
                .address(customer.getAddress())
                .mobileNumber(customer.getMobileNumber())
                .alternateMobileNumber(customer.getAlternateMobileNumber())
                .build();
    }

    @Transactional
    public Customer save(CustomerForm form) {
        String mobile = form.getMobileNumber() == null ? null : form.getMobileNumber().trim();
        Optional<Customer> duplicate = customerRepository.findByMobileNumber(mobile);
        if (duplicate.isPresent() && !duplicate.get().getId().equals(form.getId())) {
            throw new BusinessRuleException(
                    "A customer with mobile number " + mobile + " already exists (" + duplicate.get().getName() + ").");
        }

        Customer customer = form.getId() == null ? new Customer() : getEntity(form.getId());
        customer.setName(form.getName().trim());
        customer.setAddress(form.getAddress().trim());
        customer.setMobileNumber(mobile);
        customer.setAlternateMobileNumber(normalizeOptionalMobile(form.getAlternateMobileNumber()));
        return customerRepository.save(customer);
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = getEntity(id);
        if (orderRepository.existsByCustomerId(id)) {
            throw new BusinessRuleException(
                    "Cannot delete " + customer.getName() + " because they already have orders. Delete the orders first.");
        }
        customerRepository.delete(customer);
    }

    public long countOrders(Long customerId) {
        return orderRepository.countByCustomerId(customerId);
    }

    public long count() {
        return customerRepository.count();
    }

    private String normalizeOptionalMobile(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<Long, Long> orderCountsByCustomer() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : orderRepository.countGroupedByCustomer()) {
            counts.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    private CustomerView toView(Customer customer, long numberOfOrders) {
        return CustomerView.builder()
                .id(customer.getId())
                .name(customer.getName())
                .address(customer.getAddress())
                .mobileNumber(customer.getMobileNumber())
                .alternateMobileNumber(customer.getAlternateMobileNumber())
                .numberOfOrders(numberOfOrders)
                .build();
    }
}

