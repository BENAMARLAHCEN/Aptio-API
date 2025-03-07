package com.aptio.service;

import com.aptio.dto.AuthRequest;
import com.aptio.dto.AuthResponse;
import com.aptio.dto.RegisterRequest;
import com.aptio.exception.ApiException;
import com.aptio.model.Address;
import com.aptio.model.Customer;
import com.aptio.model.Role;
import com.aptio.model.User;
import com.aptio.repository.CustomerRepository;
import com.aptio.repository.RoleRepository;
import com.aptio.repository.UserRepository;
import com.aptio.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if email exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException("Email is already taken", HttpStatus.BAD_REQUEST);
        }

        // Create new user
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Set user role
        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(Role.RoleName.ROLE_USER)
                .orElseThrow(() -> new ApiException("Role not found", HttpStatus.INTERNAL_SERVER_ERROR));
        roles.add(userRole);
        user.setRoles(roles);

        // Save user
        user = userRepository.save(user);

        // Create and save a customer record linked to this user
        Customer customer = Customer.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .active(true)
                .totalVisits(0)
                .totalSpent(BigDecimal.ZERO)
                .registrationDate(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // If address details were provided, create an address for the customer
        if (request.getAddress() != null) {
            Address address = Address.builder()
                    .street(request.getAddress().getStreet())
                    .city(request.getAddress().getCity())
                    .state(request.getAddress().getState())
                    .zipCode(request.getAddress().getZipCode())
                    .country(request.getAddress().getCountry())
                    .build();
            customer.setAddress(address);
        }

        customerRepository.save(customer);

        // Generate JWT token
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtil.generateToken(
                (org.springframework.security.core.userdetails.User) authentication.getPrincipal()
        );

        // Return response
        return AuthResponse.builder()
                .token(jwt)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .toArray(String[]::new))
                .build();
    }

    public AuthResponse authenticate(AuthRequest request) {
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate JWT token
        org.springframework.security.core.userdetails.User userDetails =
                (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
        String jwt = jwtUtil.generateToken(userDetails);

        // Get user details
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        // Return response
        return AuthResponse.builder()
                .token(jwt)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toArray(String[]::new))
                .build();
    }
}