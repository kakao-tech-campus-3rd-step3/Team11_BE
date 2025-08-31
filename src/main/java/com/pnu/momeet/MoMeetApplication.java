package com.pnu.momeet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MoMeetApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoMeetApplication.class, args);
    }

}
