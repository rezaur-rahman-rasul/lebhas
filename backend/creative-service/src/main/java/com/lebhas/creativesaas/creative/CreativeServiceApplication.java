package com.lebhas.creativesaas.creative;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.lebhas.creativesaas", "com.lebhas.notification", "com.lebhas.approval", "com.lebhas.ai"})
public class CreativeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CreativeServiceApplication.class, args);
    }
}
