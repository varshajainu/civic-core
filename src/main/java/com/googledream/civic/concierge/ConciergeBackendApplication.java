package com.googledream.civic.concierge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ConciergeBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConciergeBackendApplication.class, args);
		System.out.println("Application is running");
	}

}
