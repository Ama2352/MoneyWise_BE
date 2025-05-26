package JavaProject.MoneyManagement_BE_SE330.services.impls;

import JavaProject.MoneyManagement_BE_SE330.services.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class JwtServiceImpl implements JwtService {

    @Value("${jwt.secret}")
    private String accessTokenSecret;

    @Value("${jwt.expiration}")
    private long accessTokenExpiration;

    @Override
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities());
        String jti = UUID.randomUUID().toString();
        claims.put("jti", jti);
        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(Keys.hmacShaKeyFor(accessTokenSecret.getBytes()), Jwts.SIG.HS512)
                .compact();
    }

    @Override
    public String extractUsername(String token) {
        log.info("Attempting to extract username from token: {}", token);
        if (token == null || token.isEmpty() || !isValidJwtFormat(token)) {
            log.error("Invalid JWT format: {}", token);
            throw new MalformedJwtException("Invalid JWT token: " + token);
        }
        try {
            return Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(accessTokenSecret.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (Exception e) {
            log.error("Failed to parse token: {}", e.getMessage());
            throw new MalformedJwtException("Unable to parse JWT token: " + token, e);
        }
    }

    @Override
    public String extractJwtId(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(accessTokenSecret.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("jti", String.class);
        } catch (Exception e) {
            log.error("Failed to extract jti: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            Date expiration = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(accessTokenSecret.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();
            return (username.equals(userDetails.getUsername()) && !expiration.before(new Date()));
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String validateExpiredToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(accessTokenSecret.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return e.getClaims().getSubject(); // Return username even if expired
        } catch (Exception e) {
            log.error("Failed to validate expired token: {}", e.getMessage());
            return null;
        }
    }

    private boolean isValidJwtFormat(String token) {
        int periodCount = token.length() - token.replace(".", "").length();
        return periodCount == 2;
    }

    @PostConstruct
    public void init() {
        log.info("Access Token Secret: {}", accessTokenSecret);
        log.info("Access Token Expiration: {} ms", accessTokenExpiration);
    }
}