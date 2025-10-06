package com.guardlite.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GuardliteApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuardliteApplication.class, args);
    }

}
