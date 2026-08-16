package com.erp.inventory.service.impl;

import com.erp.inventory.dto.request.CustomerRequest;
import com.erp.inventory.dto.response.CustomerResponse;
import com.erp.inventory.entity.Customer;
import com.erp.inventory.exception.ResourceNotFoundException;
import com.erp.inventory.repository.CustomerRepository;
import com.erp.inventory.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        Customer customer = Customer.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .build();
        return toResponse(customerRepository.save(customer));
    }

    @Override
    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = findEntity(id);
        customer.setName(request.getName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        customer.setAddress(request.getAddress());
        return toResponse(customer);
    }

    @Override
    public CustomerResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<CustomerResponse> getAll() {
        return customerRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        customerRepository.delete(findEntity(id));
    }

    private Customer findEntity(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", id));
    }

    private CustomerResponse toResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .address(customer.getAddress())
                .build();
    }
}
