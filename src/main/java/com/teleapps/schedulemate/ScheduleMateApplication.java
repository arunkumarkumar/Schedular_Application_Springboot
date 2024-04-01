package com.teleapps.schedulemate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.teleapps.schedulemate.util.CustomBanner;

@SpringBootApplication
public class ScheduleMateApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(ScheduleMateApplication.class);
		app.setBanner(new CustomBanner());
		app.run(args);
	}
}