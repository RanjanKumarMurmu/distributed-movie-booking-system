package com.ranjan.payment_microservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PaymentMicroserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentMicroserviceApplication.class, args);
	}

}
