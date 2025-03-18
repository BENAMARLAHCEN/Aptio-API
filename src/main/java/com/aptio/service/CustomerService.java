package com.aptio.service;

import com.aptio.dto.CustomerDTO;
import com.aptio.dto.CustomerNoteDTO;

import java.util.List;

public interface CustomerService {

    List<CustomerDTO> getAllCustomers();

    CustomerDTO getCustomerById(String id);

    CustomerDTO createCustomer(CustomerDTO customerDTO);

    CustomerDTO updateCustomer(String id, CustomerDTO customerDTO);

    void deleteCustomer(String id);

    CustomerDTO toggleCustomerStatus(String id, boolean active);

    List<CustomerDTO> searchCustomers(String query);

    CustomerNoteDTO addCustomerNote(String customerId, CustomerNoteDTO noteDTO);

    List<CustomerNoteDTO> getCustomerNotes(String customerId);
}