package com.aptio.service.impl;

import com.aptio.dto.CustomerDTO;
import com.aptio.dto.CustomerNoteDTO;
import com.aptio.exception.ResourceNotFoundException;
import com.aptio.exception.ValidationException;
import com.aptio.model.Customer;
import com.aptio.model.CustomerNote;
import com.aptio.model.Role;
import com.aptio.model.User;
import com.aptio.repository.CustomerNoteRepository;
import com.aptio.repository.CustomerRepository;
import com.aptio.repository.RoleRepository;
import com.aptio.repository.UserRepository;
import com.aptio.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerNoteRepository customerNoteRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    private static final String DEFAULT_PASSWORD = "password123"; // This should be changed or generated randomly in production

    public List<CustomerDTO> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(customer -> modelMapper.map(customer, CustomerDTO.class))
                .collect(Collectors.toList());
    }

    public CustomerDTO getCustomerById(String id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));
        return modelMapper.map(customer, CustomerDTO.class);
    }

    @Transactional
    public CustomerDTO createCustomer(CustomerDTO customerDTO) {
        // Check if email exists
        if (customerRepository.existsByEmail(customerDTO.getEmail())) {
            throw new ValidationException("Email is already in use");
        }

        // Also check if email exists in users table
        if (userRepository.existsByEmail(customerDTO.getEmail())) {
            throw new ValidationException("Email is already in use by another user");
        }

        // 1. Create a user first
        User user = new User();
        user.setFirstName(customerDTO.getFirstName());
        user.setLastName(customerDTO.getLastName());
        user.setEmail(customerDTO.getEmail());
        user.setPhone(customerDTO.getPhone());
        user.setAddress(customerDTO.getAddress());
        user.setBirthDate(customerDTO.getBirthDate());
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setActive(true);

        // Assign user role
        Role userRole = roleRepository.findByName(Role.RoleName.ROLE_USER)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "ROLE_USER"));
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);

        // 2. Now create the customer linked to this user
        Customer customer = modelMapper.map(customerDTO, Customer.class);
        customer.setRegistrationDate(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
        customer.setActive(true);

        Customer savedCustomer = customerRepository.save(customer);

        return modelMapper.map(savedCustomer, CustomerDTO.class);
    }

    @Transactional
    public CustomerDTO updateCustomer(String id, CustomerDTO customerDTO) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));

        // Check if email exists and not the same customer
        if (!customer.getEmail().equals(customerDTO.getEmail()) &&
                customerRepository.existsByEmail(customerDTO.getEmail())) {
            throw new ValidationException("Email is already in use");
        }

        // Also check if the new email exists in the users table
        if (!customer.getEmail().equals(customerDTO.getEmail()) &&
                userRepository.existsByEmail(customerDTO.getEmail())) {
            throw new ValidationException("Email is already in use by another user");
        }

        // Try to find user with the same email as the customer
        userRepository.findByEmail(customer.getEmail()).ifPresent(user -> {
            // Update user details
            user.setFirstName(customerDTO.getFirstName());
            user.setLastName(customerDTO.getLastName());
            user.setEmail(customerDTO.getEmail());
            user.setPhone(customerDTO.getPhone());
            user.setAddress(customerDTO.getAddress());
            user.setBirthDate(customerDTO.getBirthDate());
            userRepository.save(user);
        });

        // Update customer fields
        customer.setFirstName(customerDTO.getFirstName());
        customer.setLastName(customerDTO.getLastName());
        customer.setEmail(customerDTO.getEmail());
        customer.setPhone(customerDTO.getPhone());
        customer.setAddress(customerDTO.getAddress());
        customer.setBirthDate(customerDTO.getBirthDate());
        customer.setGender(customerDTO.getGender());
        customer.setProfileImage(customerDTO.getProfileImage());

        Customer updatedCustomer = customerRepository.save(customer);
        return modelMapper.map(updatedCustomer, CustomerDTO.class);
    }

    @Transactional
    public void deleteCustomer(String id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));

        // Try to find and delete the associated user
        userRepository.findByEmail(customer.getEmail()).ifPresent(userRepository::delete);

        customerRepository.deleteById(id);
    }

    @Transactional
    public CustomerDTO toggleCustomerStatus(String id, boolean active) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));

        customer.setActive(active);
        Customer updatedCustomer = customerRepository.save(customer);

        // Update user status if exists
        userRepository.findByEmail(customer.getEmail()).ifPresent(user -> {
            user.setActive(active);
            userRepository.save(user);
        });

        return modelMapper.map(updatedCustomer, CustomerDTO.class);
    }

    public List<CustomerDTO> searchCustomers(String query) {
        return customerRepository.searchCustomers(query).stream()
                .map(customer -> modelMapper.map(customer, CustomerDTO.class))
                .collect(Collectors.toList());
    }

    @Transactional
    public CustomerNoteDTO addCustomerNote(String customerId, CustomerNoteDTO noteDTO) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));

        CustomerNote note = modelMapper.map(noteDTO, CustomerNote.class);
        note.setCustomer(customer);
        note.setCreatedAt(LocalDateTime.now());

        customer.addNote(note);
        customerRepository.save(customer);

        return modelMapper.map(note, CustomerNoteDTO.class);
    }

    public List<CustomerNoteDTO> getCustomerNotes(String customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer", "id", customerId);
        }

        return customerNoteRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(note -> modelMapper.map(note, CustomerNoteDTO.class))
                .collect(Collectors.toList());
    }
}