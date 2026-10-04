package com.sigecin.serviceoffering.repository;

import com.sigecin.IntegrationTest;
import com.sigecin.serviceoffering.entity.ServiceOffering;
import com.sigecin.serviceoffering.enums.ServiceStatus;
import com.sigecin.serviceoffering.repository.ServiceOfferingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mapeo de ServiceOffering (estado y precio) y consultas por negocio.
 * Usa el esquema y los datos de prueba de ddl_sigecin.sql; cada prueba se revierte.
 */
@IntegrationTest
@Transactional
class ServiceOfferingRepositoryTests {

    @Autowired ServiceOfferingRepository services;

    @Test
    void serviceMapsStatusAndPrice() {
        assertThat(services.findByBusinessIdOrderByName(2L)).hasSize(3);
        assertThat(services.findByBusinessIdAndStatusOrderByName(2L, ServiceStatus.ACTIVE))
                .extracting(ServiceOffering::getName)
                .containsExactly("Facial hidratante", "Masaje relajante");
        ServiceOffering tinte = services.findByIdAndBusinessId(3L, 1L).orElseThrow();
        assertThat(tinte.getPrice()).isEqualByComparingTo("350.00");
        assertThat(tinte.getDurationMin()).isEqualTo(90);
        // Un servicio de otro negocio no se encuentra
        assertThat(services.findByIdAndBusinessId(3L, 2L)).isEmpty();
    }
}
