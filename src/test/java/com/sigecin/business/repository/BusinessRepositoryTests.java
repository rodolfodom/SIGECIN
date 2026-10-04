package com.sigecin.business.repository;

import com.sigecin.IntegrationTest;
import com.sigecin.business.entity.Business;
import com.sigecin.business.repository.BusinessRepository;
import com.sigecin.business.repository.BusinessScheduleRepository;
import com.sigecin.serviceoffering.enums.ServiceStatus;
import com.sigecin.serviceoffering.repository.ServiceOfferingRepository;
import com.sigecin.user.entity.User;
import com.sigecin.user.enums.Role;
import com.sigecin.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mapeo de Business y su horario, y reglas de visibilidad de BusinessRepository.
 * Usa el esquema y los datos de prueba de ddl_sigecin.sql; cada prueba se revierte.
 */
@IntegrationTest
@Transactional
class BusinessRepositoryTests {

    @Autowired UserRepository users;
    @Autowired BusinessRepository businesses;
    @Autowired BusinessScheduleRepository schedules;
    @Autowired ServiceOfferingRepository services;
    @Autowired EntityManager em;

    @Test
    void businessMapsOwnerCategoryAndSchedule() {
        User owner = users.findByEmail("barberia.estilo@unam.mx").orElseThrow();
        Business barberia = businesses.findByOwnerId(owner.getId()).orElseThrow();

        assertThat(owner.getRole()).isEqualTo(Role.BUSINESS);
        assertThat(barberia.getCategory().getName()).isEqualTo("Barbería");
        assertThat(barberia.getSchedules()).hasSize(6);
        assertThat(barberia.getSchedules().get(0).getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(schedules.findByBusinessIdAndDayOfWeekAndActiveTrue(barberia.getId(), DayOfWeek.SATURDAY))
                .get().extracting(s -> s.getCloseTime()).isEqualTo(LocalTime.of(15, 0));
        assertThat(schedules.findByBusinessIdAndDayOfWeekAndActiveTrue(barberia.getId(), DayOfWeek.SUNDAY)).isEmpty();
        assertThat(businesses.findByIdForUpdate(barberia.getId())).isPresent();
    }

    @Test
    void searchReturnsOnlyVisibleBusinesses() {
        var all = PageRequest.of(0, 10);

        assertThat(businesses.searchVisible(null, null, all).getTotalElements()).isEqualTo(3);
        assertThat(businesses.searchVisible("SPA", null, all))
                .extracting(Business::getName).containsExactly("Spa Serenidad");
        assertThat(businesses.searchVisible(null, 4, all))
                .extracting(Business::getName).containsExactly("Clínica Dental Sonrisa");

        // Un negocio inactivo deja de ser visible
        Business spa = businesses.searchVisible("Spa", null, all).getContent().get(0);
        spa.setActive(false);
        em.flush();
        assertThat(businesses.searchVisible(null, null, all).getTotalElements()).isEqualTo(2);
        assertThat(businesses.findVisibleById(spa.getId())).isEmpty();
    }

    @Test
    void businessWithoutActiveServicesIsNotVisible() {
        Business barberia = businesses.findById(1L).orElseThrow();
        services.findByBusinessIdOrderByName(barberia.getId())
                .forEach(s -> s.setStatus(ServiceStatus.INACTIVE));
        em.flush();

        assertThat(businesses.findVisibleById(barberia.getId())).isEmpty();
    }
}
