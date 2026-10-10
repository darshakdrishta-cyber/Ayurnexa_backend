package com.Ayurnexa.Ayurnexa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;

@SpringBootApplication
public class AyurnexaApplication {

	public static void main(String[] args) {
		SpringApplication.run(AyurnexaApplication.class, args);
	}


	@GetMapping ("/")
	public String home()
	{
		return "Home World";
	}

}
