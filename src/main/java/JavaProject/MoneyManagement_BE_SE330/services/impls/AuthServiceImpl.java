package JavaProject.MoneyManagement_BE_SE330.services.impls;

import JavaProject.MoneyManagement_BE_SE330.helper.ApplicationMapper;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.auth.AuthResponseDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.auth.RegisterDTO;
import JavaProject.MoneyManagement_BE_SE330.models.entities.RefreshToken;
import JavaProject.MoneyManagement_BE_SE330.models.entities.User;
import JavaProject.MoneyManagement_BE_SE330.repositories.UserRepository;
import JavaProject.MoneyManagement_BE_SE330.services.AuthService;
import JavaProject.MoneyManagement_BE_SE330.services.JwtService;
import JavaProject.MoneyManagement_BE_SE330.services.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
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
    private final RefreshTokenService refreshTokenService;

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Z])(?=.*[@#$%^&+=!]).{6,}$");

    @Override
    public AuthResponseDTO authenticate(String email, String password) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");

        }
        String accessToken = jwtService.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(email);

        AuthResponseDTO response = new AuthResponseDTO();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken.getToken());
        return response;
    }

    @Override
    public boolean register(RegisterDTO dto) {
        String email = dto.getEmail();
        String password = dto.getPassword();
        String confirmedPassword = dto.getConfirmedPassword();

        if (userRepository.existsByEmail(email)) {
            return false;
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            return false;
        }

        if (!password.equals(confirmedPassword)) {
            return false;
        }

        User newUser = applicationMapper.toUserEntity(dto);
        userRepository.save(newUser);

        return true;
    }
}