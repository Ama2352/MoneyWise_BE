    package JavaProject.MoneyManagement_BE_SE330.services;

    import org.springframework.security.core.userdetails.UserDetails;

    public interface JwtService {
        String generateToken(UserDetails userDetails);
        String extractUsername(String token);
        String extractJwtId(String token);
        boolean isTokenValid(String token, UserDetails userDetails);
        String validateExpiredToken(String token);
    }