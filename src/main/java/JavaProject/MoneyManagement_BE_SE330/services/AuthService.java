package JavaProject.MoneyManagement_BE_SE330.services;

import JavaProject.MoneyManagement_BE_SE330.helper.ApplicationMapper;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.RegisterDTO;
import JavaProject.MoneyManagement_BE_SE330.models.entities.User;
import JavaProject.MoneyManagement_BE_SE330.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
public class AuthService {
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ApplicationMapper applicationMapper;

    public String authenticate(String email, String password) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        if(!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
        return jwtService.generateToken(userDetails);
    }

    // Regex: At least 1 uppercase, 1 special character, minimum 6 characters
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Z])(?=.*[@#$%^&+=!]).{6,}$");

    public boolean register(RegisterDTO dto) {
        String email = dto.getEmail();
        String password = dto.getPassword();
        String confirmedPassword = dto.getConfirmedPassword();

        // Check if email already exists
        if(userRepository.existsByEmail(email)) {
//            throw new IllegalArgumentException("Email already exists");
            return false;
        }

        // Validate password format
        if(!PASSWORD_PATTERN.matcher(password).matches()) {
//            throw new IllegalArgumentException("Password must be at least 6 characters, contain 1 uppercase letter, and 1 special character (@#$%^&+=!)");
            return false;
        }

        // Validate password match
        if(!password.equals(confirmedPassword)) {
            return false;
        }

        User newUser = applicationMapper.toUserEntity(dto);

        userRepository.save(newUser);

        return true;
    }
}
