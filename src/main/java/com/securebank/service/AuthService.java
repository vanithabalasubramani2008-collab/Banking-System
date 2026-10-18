package com.securebank.service;

import com.securebank.dto.AuthResponse;
import com.securebank.dto.LoginRequest;
import com.securebank.dto.RegisterRequest;
import com.securebank.entity.User;
import com.securebank.exception.AccountLockedException;
import com.securebank.exception.InvalidTransactionException;
import com.securebank.repository.UserRepository;
import com.securebank.security.JwtTokenProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new InvalidTransactionException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new InvalidTransactionException("Email '" + request.getEmail() + "' is already registered");
        }

        // Hash both password and PIN with BCrypt - Never store plaintext!
        String passwordHash = passwordEncoder.encode(request.getPassword());
        String pinHash = passwordEncoder.encode(request.getPin());

        User user = new User(
                request.getUsername(),
                request.getEmail(),
                passwordHash,
                pinHash,
                request.getFullName(),
                request.getPhoneNumber(),
                "ROLE_CUSTOMER"
        );

        userRepository.save(user);

        String token = tokenProvider.generateToken(user.getUsername(), user.getRole());
        return new AuthResponse(token, user.getUsername(), user.getRole(), "User registered successfully");
    }

    @Transactional(noRollbackFor = {BadCredentialsException.class, AccountLockedException.class})
    public AuthResponse login(LoginRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
        if (userOpt.isEmpty()) {
            throw new BadCredentialsException("Invalid username or password");
        }

        User user = userOpt.get();

        if (!user.isAccountNonLocked()) {
            throw new AccountLockedException("Your account is locked due to 3 consecutive failed login attempts. Please contact bank administration.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            user.incrementFailedAttempts();
            userRepository.saveAndFlush(user);

            int remaining = 3 - user.getFailedLoginAttempts();
            if (remaining > 0) {
                throw new BadCredentialsException("Invalid username or password. Remaining attempts before lock: " + remaining);
            } else {
                throw new AccountLockedException("Account locked! You have exceeded 3 failed login attempts.");
            }
        }

        // On successful authentication, reset failed attempts
        user.resetFailedAttempts();
        userRepository.save(user);

        String token = tokenProvider.generateToken(user.getUsername(), user.getRole());
        return new AuthResponse(token, user.getUsername(), user.getRole(), "Login successful");
    }

    public User getCurrentAuthenticatedUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found: " + username));
    }

    public void verifyPin(User user, String rawPin) {
        if (rawPin == null || !passwordEncoder.matches(rawPin, user.getPinHash())) {
            throw new InvalidTransactionException("Invalid transaction PIN provided");
        }
    }
}
