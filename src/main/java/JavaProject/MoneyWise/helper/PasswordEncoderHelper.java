package JavaProject.MoneyWise.helper;

import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PasswordEncoderHelper {

    private final PasswordEncoder passwordEncoder;

    @Named("encode")
    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

}
