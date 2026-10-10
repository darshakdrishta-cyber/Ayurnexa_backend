package com.Ayurnexa.Ayurnexa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RequestMapping("/api/")
@RestController
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
