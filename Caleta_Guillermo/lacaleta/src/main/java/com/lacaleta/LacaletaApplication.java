package com.lacaleta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class LacaletaApplication {

	public static void main(String[] args) {
		SpringApplication.run(LacaletaApplication.class, args);
	}

}
