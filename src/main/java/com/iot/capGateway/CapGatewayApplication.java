package com.iot.capGateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.iot.capGateway")
public class CapGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(CapGatewayApplication.class, args);
	}

}
