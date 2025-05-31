package JavaProject.MoneyManagement_BE_SE330.services.impls;

import JavaProject.MoneyManagement_BE_SE330.helper.ApplicationMapper;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.auth.RegisterDTO;
import JavaProject.MoneyManagement_BE_SE330.models.entities.User;
import JavaProject.MoneyManagement_BE_SE330.repositories.UserRepository;
import JavaProject.MoneyManagement_BE_SE330.services.AuthService;
import JavaProject.MoneyManagement_BE_SE330.services.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
public class AuthServiceImpl implements AuthService {
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ApplicationMapper applicationMapper;

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Z])(?=.*[@#$%^&+=!]).{6,}$");

    @Override
    public String authenticate(String email, String password) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return jwtService.generateToken(userDetails);
    }

    @Override
    public boolean register(RegisterDTO dto) {
        String email = dto.getEmail();
        String password = dto.getPassword();
        String confirmPassword = dto.getConfirmPassword();

        if (userRepository.existsByEmail(email)) {
            return false;
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            return false;
        }

        if (!password.equals(confirmPassword)) {
            return false;
        }

        User newUser = applicationMapper.toUserEntity(dto);

        try {
            userRepository.save(newUser);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}