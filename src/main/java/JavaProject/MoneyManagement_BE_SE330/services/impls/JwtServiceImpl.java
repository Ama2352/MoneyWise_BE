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

@Slf4j
@Service
public class JwtServiceImpl implements JwtService {

    @Value("${jwt.secret}")
    private String accessTokenSecret;

    @Value("${jwt.refresh-token.secret}")
    private String refreshTokenSecret; // Separate secret for refresh tokens

    @Value("${jwt.expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;

    @Override
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities());
        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(Keys.hmacShaKeyFor(accessTokenSecret.getBytes()), Jwts.SIG.HS512)
                .compact();
    }

    @Override
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities());
        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(Keys.hmacShaKeyFor(refreshTokenSecret.getBytes()), Jwts.SIG.HS512)
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
            log.error("Failed to parse with access token secret: {}", e.getMessage());
            try {
                return Jwts.parser()
                        .verifyWith(Keys.hmacShaKeyFor(refreshTokenSecret.getBytes()))
                        .build()
                        .parseSignedClaims(token)
                        .getPayload()
                        .getSubject();
            } catch (Exception ex) {
                log.error("Failed to parse with refresh token secret: {}", ex.getMessage());
                throw new MalformedJwtException("Unable to parse JWT token: " + token, ex);
            }
        }
    }

    private boolean isValidJwtFormat(String token) {
        int periodCount = token.length() - token.replace(".", "").length();
        return periodCount == 2;
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
            // Check if it's a refresh token
            try {
                String username = Jwts.parser()
                        .verifyWith(Keys.hmacShaKeyFor(refreshTokenSecret.getBytes()))
                        .build()
                        .parseSignedClaims(token)
                        .getPayload()
                        .getSubject();
                Date expiration = Jwts.parser()
                        .verifyWith(Keys.hmacShaKeyFor(refreshTokenSecret.getBytes()))
                        .build()
                        .parseSignedClaims(token)
                        .getPayload()
                        .getExpiration();
                return (username.equals(userDetails.getUsername()) && !expiration.before(new Date()));
            } catch (Exception ex) {
                return false;
            }
        }
    }

    @PostConstruct
    public void init() {
        log.info("Access Token Secret: {}", accessTokenSecret);
        log.info("Refresh Token Secret: {}", refreshTokenSecret);
        log.info("Access Token Expiration: {} ms", accessTokenExpiration);
        log.info("Refresh Token Expiration: {} ms", refreshTokenExpiration);
    }
}