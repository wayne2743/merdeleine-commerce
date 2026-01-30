package com.merdeleine.production;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ThresholdServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ThresholdServiceApplication.class, args);
    }
}
