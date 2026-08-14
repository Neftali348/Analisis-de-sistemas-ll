package com.umg.sgq.seguridad;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Date;

@Service
public class ServicioJwt {
    private final SecretKey key;
    private final long expirationHours;

    public ServicioJwt(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.absolute-expiration-hours:8}") long expirationHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationHours = expirationHours;
    }

    public String generate(String username, long credentialVersion) {
        Instant now = Instant.now();
        return Jwts.builder().subject(username).claim("cv", credentialVersion).issuedAt(Date.from(now)).expiration(Date.from(now.plus(Duration.ofHours(expirationHours)))).signWith(key).compact();
    }

    public String username(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    }

    public long credentialVersion(String token) {
        Number n = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().get("cv", Number.class);
        return n == null ? 0L : n.longValue();
    }

    public boolean valid(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
