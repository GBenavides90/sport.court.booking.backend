package com.sport.court.booking.service;

import com.sport.court.booking.domain.Role;
import com.sport.court.booking.domain.User;
import com.sport.court.booking.dto.AuthResponse;
import com.sport.court.booking.dto.LoginRequest;
import com.sport.court.booking.dto.RegisterRequest;
import com.sport.court.booking.dto.UserDto;
import com.sport.court.booking.exception.ConflictException;
import com.sport.court.booking.repository.UserRepository;
import com.sport.court.booking.security.JwtService;
import com.sport.court.booking.security.TokenBlacklist;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklist blacklist;
    private final EmailService emailService;

    /** HU13 — registra un usuario con rol USER y dispara el correo de confirmación (HU19). */
    public UserDto register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Ya existe una cuenta registrada con ese correo electrónico");
        }
        User user = new User();
        user.setFirstName(req.firstName().trim());
        user.setLastName(req.lastName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setRole(Role.USER);
        User saved = userRepository.save(user);
        emailService.sendRegistrationConfirmation(saved);
        return UserDto.from(saved);
    }

    /** HU14 — autentica con correo y contraseña. El mensaje de error es genérico a propósito. */
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmailIgnoreCase(req.email().trim())
                .filter(u -> passwordEncoder.matches(req.password(), u.getPassword()))
                .orElseThrow(() -> new BadCredentialsException("Correo electrónico o contraseña incorrectos"));
        return new AuthResponse(jwtService.generateToken(user), UserDto.from(user));
    }

    /** HU15 — revoca el token para que deje de ser válido aunque el cliente lo conserve. */
    public void logout(String token) {
        try {
            Claims claims = jwtService.parse(token);
            blacklist.revoke(claims.getId(), claims.getExpiration());
        } catch (JwtException | IllegalArgumentException ignored) {
            // Token ya inválido: nada que revocar.
        }
    }

    /** HU19 — reenvío del correo. No revela si el correo existe. */
    public void resendConfirmation(String email) {
        userRepository.findByEmailIgnoreCase(email.trim()).ifPresent(emailService::sendRegistrationConfirmation);
    }
}
