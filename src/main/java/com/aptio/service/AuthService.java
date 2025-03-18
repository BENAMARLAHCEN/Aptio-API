package com.aptio.service;

import com.aptio.dto.AuthRequest;
import com.aptio.dto.AuthResponse;
import com.aptio.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse authenticate(AuthRequest request);
}