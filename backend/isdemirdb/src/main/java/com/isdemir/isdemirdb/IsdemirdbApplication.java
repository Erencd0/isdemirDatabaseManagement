package com.isdemir.isdemirdb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling: suresi dolmus refresh tokenlari temizleyen @Scheduled gorevi icin.
@SpringBootApplication
@EnableScheduling
public class IsdemirdbApplication {

	public static void main(String[] args) {
		SpringApplication.run(IsdemirdbApplication.class, args);
	}

}
