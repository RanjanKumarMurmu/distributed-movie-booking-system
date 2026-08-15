package com.ranjan.ordermicroservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OrdermicroserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrdermicroserviceApplication.class, args);
	}

}
