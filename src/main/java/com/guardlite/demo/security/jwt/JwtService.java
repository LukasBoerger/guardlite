package com.guardlite.demo.security.jwt;

import com.guardlite.demo.entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final Key key;
    private final long ttlMillis;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.ttlMillis:86400000}") long ttlMillis) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.ttlMillis = ttlMillis;
    }

    public String generate(String username, Map<String, Object> claims) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + ttlMillis))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateToken(String username) {
        return generate(username, Map.of()); // ohne extra Claims
    }

    public String generateToken(User user) {
        return generate(
                user.getUsername(),
                Map.of(
                        "uid", user.getId().toString(),
                        "role", user.getRole().name()
                )
        );
    }

    public String extractUsername(String token) {
        return parse(token).getBody().getSubject();
    }

    public boolean isTokenValid(String token, String username) {
        var claims = parse(token).getBody();
        return username.equals(claims.getSubject()) && claims.getExpiration().after(new Date());
    }

    private Jws<Claims> parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }
}
