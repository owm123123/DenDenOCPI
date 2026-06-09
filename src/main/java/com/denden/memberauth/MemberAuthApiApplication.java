package com.denden.memberauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MemberAuthApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(MemberAuthApiApplication.class, args);
	}

}
