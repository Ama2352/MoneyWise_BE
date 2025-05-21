package JavaProject.MoneyManagement_BE_SE330.services;

import JavaProject.MoneyManagement_BE_SE330.helper.ApplicationMapper;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.auth.RegisterDTO;
import JavaProject.MoneyManagement_BE_SE330.models.entities.User;
import JavaProject.MoneyManagement_BE_SE330.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationMapper applicationMapper;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_emailAlreadyExists_returnsFalse() {
        RegisterDTO dto = new RegisterDTO(
                "Test",
                "Dev",
                "test@example.com",
                "Test@123",
                "Test@123"
        );

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        boolean result = authService.register(dto);

        assertFalse(result); // test failed if result is true
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_invalidPassword_returnsFalse() {
        RegisterDTO dto = new RegisterDTO(
                "Test",
                "Dev",
                "test@example.com",
                "Test", // doesn't matches regex
                "Test"
        );

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);

        boolean result = authService.register(dto);

        assertFalse(result);
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_passwordsDoNotMatch_returnsFalse() {
        RegisterDTO dto = new RegisterDTO(
                "Test",
                "Dev",
                "test@example.com",
                "Test@123",
                "Test"
        );

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);

        boolean result = authService.register(dto);

        assertFalse(result);
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_validData_savesUser_returnsTrue() {
        RegisterDTO dto = new RegisterDTO(
                "Test",
                "Dev",
                "test@example.com",
                "Test@123",
                "Test@123"
        );
        User newUser = new User();

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(applicationMapper.toUserEntity(dto)).thenReturn(newUser);

        boolean result = authService.register(dto);

        assertTrue(result);
        verify(userRepository).existsByEmail("test@example.com");
        verify(applicationMapper).toUserEntity(dto);
        verify(userRepository).save(newUser);
    }
}
