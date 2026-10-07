package com.sport.court.booking.security;

import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** HU15 — Lista de tokens revocados (logout efectivo). Se limpia automáticamente al expirar cada token. */
@Component
public class TokenBlacklist {

    private final Map<String, Date> revoked = new ConcurrentHashMap<>();

    public void revoke(String jti, Date expiration) {
        purgeExpired();
        revoked.put(jti, expiration);
    }

    public boolean isRevoked(String jti) {
        return revoked.containsKey(jti);
    }

    private void purgeExpired() {
        Date now = new Date();
        revoked.entrySet().removeIf(e -> e.getValue().before(now));
    }
}
