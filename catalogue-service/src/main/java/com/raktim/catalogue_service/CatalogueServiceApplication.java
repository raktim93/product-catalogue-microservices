package com.raktim.catalogue_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@ConfigurationPropertiesScan
@SpringBootApplication
@EnableScheduling
public class CatalogueServiceApplication {



	public static void main(String[] args) {

		// Before Spring starts, so every database connection uses the current zone name.
		// Java on Windows reports the old name "Asia/Calcutta", which the PostgreSQL image rejects.
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));

		SpringApplication.run(CatalogueServiceApplication.class, args);
	}

}
