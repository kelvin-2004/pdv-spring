package PDV.PDV;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PdvApplication {

	public static void main(String[] args) {
		SpringApplication.run(PdvApplication.class, args);
	}

}

