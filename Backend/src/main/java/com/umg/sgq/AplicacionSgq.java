package com.umg.sgq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AplicacionSgq {
    public static void main(String[] args) {
        SpringApplication.run(AplicacionSgq.class, args);
    }
}
