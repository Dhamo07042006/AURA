package com.example.springapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan({"com.example.springapp", "com.aura.app"})
@EntityScan(basePackages = {"com.example.springapp.model", "com.aura.app.model"})
@EnableJpaRepositories(basePackages = {"com.example.springapp.repository", "com.aura.app.repository"})
public class SpringappApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringappApplication.class, args);
    }
}
