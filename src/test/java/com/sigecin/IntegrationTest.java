package com.sigecin;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Prueba de integración: contexto completo de Spring con MariaDB en Testcontainers.
 * Todas las clases que la usan comparten el mismo contexto (y el mismo contenedor).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(TestcontainersConfiguration.class)
@SpringBootTest
public @interface IntegrationTest {
}
