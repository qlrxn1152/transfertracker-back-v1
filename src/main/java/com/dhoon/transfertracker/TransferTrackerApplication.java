package com.dhoon.transfertracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;


@SpringBootApplication
@ConfigurationPropertiesScan
public class TransferTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TransferTrackerApplication.class, args);
	}

}
