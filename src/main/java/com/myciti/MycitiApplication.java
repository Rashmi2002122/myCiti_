package com.myciti;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MycitiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MycitiApplication.class, args);
    }
}
