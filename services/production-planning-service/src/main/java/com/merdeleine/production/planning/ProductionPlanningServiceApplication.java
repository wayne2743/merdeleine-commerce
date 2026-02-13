package com.merdeleine.production.planning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ProductionPlanningServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductionPlanningServiceApplication.class, args);
    }
}
