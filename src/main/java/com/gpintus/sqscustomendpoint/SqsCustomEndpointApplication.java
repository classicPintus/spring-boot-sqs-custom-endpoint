package com.gpintus.sqscustomendpoint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SqsCustomEndpointApplication {

	public static void main(String[] args) {
		SpringApplication.run(SqsCustomEndpointApplication.class, args);
	}

}
