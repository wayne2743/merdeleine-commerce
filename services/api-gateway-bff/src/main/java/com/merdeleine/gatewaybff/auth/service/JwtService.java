package com.merdeleine.gatewaybff.auth.service;

import com.merdeleine.gatewaybff.auth.entity.AppRole;
import com.merdeleine.gatewaybff.auth.entity.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs
    ) {
        // HS256 需要至少 256 bits (32 bytes)
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /** OAuth2 登入成功後，核發 JWT */
    public String generateToken(AppUser user, List<AppRole> roles) {
        List<String> roleCodes = roles.stream()
                .map(AppRole::getCode)
                .collect(Collectors.toList());

        return Jwts.builder()
                .subject(user.getId().toString())           // sub = userId (UUID)
                .claim("email", user.getEmail())
                .claim("displayName", user.getDisplayName())
                .claim("roles", roleCodes)                  // ["USER", "ADMIN"]
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    /** 解析並驗證 JWT，回傳 Claims */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}