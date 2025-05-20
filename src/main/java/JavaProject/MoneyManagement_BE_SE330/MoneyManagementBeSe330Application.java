package JavaProject.MoneyManagement_BE_SE330;

import JavaProject.MoneyManagement_BE_SE330.services.JwtService;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.crypto.SecretKey;
import java.util.Base64;

@SpringBootApplication
public class MoneyManagementBeSe330Application {

	public static void main(String[] args) {
		SpringApplication.run(MoneyManagementBeSe330Application.class, args);
	}
}
