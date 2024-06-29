package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        System.out.println("Starting Spring Boot application...");
        SpringApplication.run(Main.class, args);
        System.out.println("Spring Boot application started.");
    }
}
