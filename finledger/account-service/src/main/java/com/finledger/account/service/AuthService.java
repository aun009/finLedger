package com.finledger.account.service;

import com.finledger.account.dto.AuthResponse;
import com.finledger.account.dto.LoginRequest;
import com.finledger.account.dto.RegisterRequest;
import com.finledger.account.entity.AppUser;
import com.finledger.account.repository.AppUserRepository;
import com.finledger.account.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AppUserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim().toLowerCase();
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("username is already registered");
        }
        AppUser user = userRepository.save(new AppUser(username,
                passwordEncoder.encode(request.password()), "USER"));
        return tokenFor(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        AppUser user = userRepository.findByUsername(request.username().trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("invalid username or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("invalid username or password");
        }
        return tokenFor(user);
    }

    private AuthResponse tokenFor(AppUser user) {
        return new AuthResponse(jwtService.issue(user.getUsername(), user.getRole()),
                "Bearer", jwtService.expiresInSeconds());
    }
}
