package it.frusso.springboot;

import org.springframework.boot.Banner.Mode;
import org.springframework.boot.SpringApplication;

@org.springframework.boot.autoconfigure.SpringBootApplication
public class SpringBootApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(SpringBootApplication.class);
        //application.setBannerMode(Mode.CONSOLE);
        application.run(args);
	}
}
