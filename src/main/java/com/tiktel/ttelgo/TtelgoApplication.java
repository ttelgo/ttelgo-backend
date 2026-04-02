package com.tiktel.ttelgo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = "com.tiktel.ttelgo")
public class TtelgoApplication {

	public static void main(String[] args) {
		SpringApplication.run(TtelgoApplication.class, args);
	}

}

