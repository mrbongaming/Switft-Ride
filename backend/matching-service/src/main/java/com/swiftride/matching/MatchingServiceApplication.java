package com.swiftride.matching;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableRabbit
@SpringBootApplication
public class MatchingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MatchingServiceApplication.class, args);
    }
}
