package com.sport.court.booking.security;

import com.sport.court.booking.domain.Role;
import com.sport.court.booking.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas unitarias de JWT y de la lista de tokens revocados (soporte de HU14/HU15). */
class JwtServiceTest {

    private static User user() {
        return new User(1L, "Ana", "Pérez", "ana@test.com", "hash", Role.ADMIN);
    }

    @Test
    @DisplayName("El token generado contiene correo, rol y un id único")
    void generateAndParse() {
        JwtService jwt = new JwtService("secreto", 60_000);
        Claims claims = jwt.parse(jwt.generateToken(user()));
        assertThat(claims.getSubject()).isEqualTo("ana@test.com");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(claims.getId()).isNotBlank();
        assertThat(jwt.generateToken(user())).isNotEqualTo(jwt.generateToken(user()));
    }

    @Test
    @DisplayName("Un token firmado con otro secreto o alterado es rechazado")
    void rejectsTamperedOrForeign() {
        JwtService jwt = new JwtService("secreto", 60_000);
        String foreign = new JwtService("otro-secreto", 60_000).generateToken(user());
        assertThatThrownBy(() -> jwt.parse(foreign)).isInstanceOf(JwtException.class);
        String token = jwt.generateToken(user());
        assertThatThrownBy(() -> jwt.parse(token + "x")).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Un token expirado es rechazado")
    void rejectsExpired() {
        JwtService jwt = new JwtService("secreto", -1_000);
        assertThatThrownBy(() -> jwt.parse(jwt.generateToken(user()))).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("TokenBlacklist revoca por id y purga tokens ya expirados")
    void blacklist() {
        TokenBlacklist blacklist = new TokenBlacklist();
        blacklist.revoke("abc", new Date(System.currentTimeMillis() + 60_000));
        assertThat(blacklist.isRevoked("abc")).isTrue();
        assertThat(blacklist.isRevoked("otro")).isFalse();
        blacklist.revoke("viejo", new Date(System.currentTimeMillis() - 1_000));
        blacklist.revoke("nuevo", new Date(System.currentTimeMillis() + 60_000)); // dispara la purga
        assertThat(blacklist.isRevoked("viejo")).isFalse();
    }
}
