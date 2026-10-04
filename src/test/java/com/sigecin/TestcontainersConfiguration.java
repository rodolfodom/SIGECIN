package com.sigecin;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Levanta un MariaDB en Docker para las pruebas y carga ddl_sigecin.sql.
 * Usa la versión exigida para el proyecto (10.7.3), de modo que el esquema, las vistas,
 * los bloqueos y las pruebas de concurrencia se validan contra ella.
 * El script se monta en docker-entrypoint-initdb.d porque usa DELIMITER
 * (procedimiento almacenado), que solo entiende el cliente mariadb.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MariaDBContainer<?> mariaDbContainer() {
        return new MariaDBContainer<>(DockerImageName.parse("mariadb:10.7.3"))
                .withDatabaseName("sigecin")
                // Desfase fijo y no "America/Mexico_City": la imagen 10.7.3 trae datos de zonas
                // horarias anteriores a 2022 y aplicaría horario de verano (México lo eliminó)
                .withCommand("--default-time-zone=-06:00")
                .withCopyFileToContainer(
                        MountableFile.forHostPath("db/ddl_sigecin.sql"),
                        "/docker-entrypoint-initdb.d/ddl_sigecin.sql");
    }
}
