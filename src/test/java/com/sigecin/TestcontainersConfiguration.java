package com.sigecin;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Levanta un MariaDB en Docker para las pruebas y carga ddl_sigecin.sql.
 * El script se monta en docker-entrypoint-initdb.d porque usa DELIMITER
 * (procedimiento almacenado), que solo entiende el cliente mariadb.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MariaDBContainer<?> mariaDbContainer() {
        return new MariaDBContainer<>(DockerImageName.parse("mariadb:11.4"))
                .withDatabaseName("sigecin")
                .withEnv("TZ", "America/Mexico_City")
                .withCopyFileToContainer(
                        MountableFile.forHostPath("db/ddl_sigecin.sql"),
                        "/docker-entrypoint-initdb.d/ddl_sigecin.sql");
    }
}
