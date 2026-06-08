package com.skala.chip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
@EnableScheduling
@RestController
public class SkalaChipApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkalaChipApplication.class, args);
    }

    @GetMapping("/")
    public String root() {
        return "skala-chip-backend is running";
    }

    @GetMapping("/health")
    public String health() {
        return "ok";
    }
}
