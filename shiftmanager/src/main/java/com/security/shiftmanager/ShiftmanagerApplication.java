package com.security.shiftmanager;

import com.microsoft.applicationinsights.attach.ApplicationInsights;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ShiftmanagerApplication {

	public static void main(String[] args) {
		// Sends requests, SQL, logs and Micrometer metrics to Application Insights (only in Azure, where the connection string is set).
		if (System.getenv("APPLICATIONINSIGHTS_CONNECTION_STRING") != null) {
			ApplicationInsights.attach();
		}
		SpringApplication.run(ShiftmanagerApplication.class, args);
	}

}
