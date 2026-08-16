package com.ranjan.seat_microservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SeatMicroserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SeatMicroserviceApplication.class, args);
	}

}
