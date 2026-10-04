package com.sigecin;

import org.springframework.boot.SpringApplication;

/**
 * Arranca la aplicación en local con MariaDB en Docker (útil sin una BD instalada).
 */
public class TestSigecinApplication {

    public static void main(String[] args) {
        SpringApplication.from(SigecinApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
