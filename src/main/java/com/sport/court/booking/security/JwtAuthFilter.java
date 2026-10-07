package com.sport.court.booking.security;

import com.sport.court.booking.domain.User;
import com.sport.court.booking.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lee el encabezado Authorization: Bearer, valida el token y carga al usuario desde la BD en cada petición,
 * de modo que un cambio de rol (HU16) o un logout (HU15) tengan efecto inmediato.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenBlacklist blacklist;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtService.parse(header.substring(7));
                if (!blacklist.isRevoked(claims.getId())) {
                    userRepository.findByEmailIgnoreCase(claims.getSubject()).ifPresent(this::authenticate);
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                // Token inválido o expirado: la petición sigue sin autenticar y será rechazada si el recurso es protegido.
            }
        }
        chain.doFilter(request, response);
    }

    private void authenticate(User user) {
        var auth = new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
