package com.sigecin.user.repository;

import com.sigecin.IntegrationTest;
import com.sigecin.business.entity.Business;
import com.sigecin.user.entity.User;
import com.sigecin.user.enums.Role;
import com.sigecin.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mapeo de User (rol y favoritos) y consultas de UserRepository.
 * Usa el esquema y los datos de prueba de ddl_sigecin.sql; cada prueba se revierte.
 */
@IntegrationTest
@Transactional
class UserRepositoryTests {

    @Autowired UserRepository users;

    @Test
    void userMapsRoleAndFavorites() {
        User carlos = users.findByEmail("carlos.ramirez@unam.mx").orElseThrow();

        assertThat(carlos.getRole()).isEqualTo(Role.CLIENT);
        assertThat(carlos.isActive()).isTrue();
        assertThat(carlos.getCreatedAt()).isNotNull();
        assertThat(carlos.getFavoriteBusinesses())
                .extracting(Business::getName)
                .containsExactlyInAnyOrder("Barbería El Estilo", "Clínica Dental Sonrisa");
        assertThat(users.findByEmail("sofia.herrera@unam.mx").orElseThrow().isActive()).isFalse();
    }
}
