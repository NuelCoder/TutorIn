package com.TutorIn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TutorInApplication {

	public static void main(String[] args) {
		SpringApplication.run(TutorInApplication.class, args);
	}

}
