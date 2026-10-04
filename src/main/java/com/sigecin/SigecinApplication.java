package com.sigecin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class SigecinApplication {

    public static void main(String[] args) {
        // La zona horaria de la JVM debe coincidir con la de MariaDB ("hoy" y "mes en curso" dependen de ella)
        TimeZone.setDefault(TimeZone.getTimeZone("America/Mexico_City"));
        SpringApplication.run(SigecinApplication.class, args);
    }
}
