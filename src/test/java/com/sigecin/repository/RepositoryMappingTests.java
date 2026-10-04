package com.sigecin.repository;

import com.sigecin.TestcontainersConfiguration;
import com.sigecin.appointment.Appointment;
import com.sigecin.appointment.AppointmentStatus;
import com.sigecin.appointment.CancellationReason;
import com.sigecin.appointment.CancelledBy;
import com.sigecin.appointment.repository.AppointmentRepository;
import com.sigecin.appointment.repository.CancellationReasonRepository;
import com.sigecin.business.Business;
import com.sigecin.business.repository.BusinessRepository;
import com.sigecin.business.repository.BusinessScheduleRepository;
import com.sigecin.serviceoffering.ServiceOffering;
import com.sigecin.serviceoffering.ServiceStatus;
import com.sigecin.serviceoffering.repository.ServiceOfferingRepository;
import com.sigecin.user.Role;
import com.sigecin.user.User;
import com.sigecin.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica el mapeo de entidades y las consultas de los repositorios contra
 * el esquema y los datos de prueba de ddl_sigecin.sql. Cada prueba se revierte.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class RepositoryMappingTests {

    // Cita histórica del script: Barbería (negocio 1), 2026-04-21 10:00–10:30, CONFIRMED
    private static final LocalDateTime APRIL_21_10AM = LocalDateTime.of(2026, 4, 21, 10, 0);

    @Autowired UserRepository users;
    @Autowired BusinessRepository businesses;
    @Autowired BusinessScheduleRepository schedules;
    @Autowired ServiceOfferingRepository services;
    @Autowired AppointmentRepository appointments;
    @Autowired CancellationReasonRepository reasons;
    @Autowired EntityManager em;

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

    @Test
    void businessMapsOwnerCategoryAndSchedule() {
        User owner = users.findByEmail("barberia.estilo@unam.mx").orElseThrow();
        Business barberia = businesses.findByOwnerId(owner.getId()).orElseThrow();

        assertThat(owner.getRole()).isEqualTo(Role.BUSINESS);
        assertThat(barberia.getCategory().getName()).isEqualTo("Barbería");
        assertThat(barberia.getSchedules()).hasSize(6);
        assertThat(barberia.getSchedules().getFirst().getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
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
        Business spa = businesses.searchVisible("Spa", null, all).getContent().getFirst();
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

    @Test
    void cancellationReasonsAreFilteredByActor() {
        assertThat(reasons.findByCancelledByOrderByName(CancelledBy.CLIENT)).hasSize(4);
        assertThat(reasons.findByCancelledByOrderByName(CancelledBy.BUSINESS)).hasSize(3);
        assertThat(reasons.findByCancelledByOrderByName(CancelledBy.SYSTEM)).hasSize(1);
        // Un motivo de cliente no lo puede usar el negocio
        assertThat(reasons.findByIdAndCancelledBy(1, CancelledBy.BUSINESS)).isEmpty();
    }

    @Test
    void overlapIgnoresCancelledAndAdjacentAppointments() {
        // Traslape con la cita de 10:00–10:30
        assertThat(appointments.existsOverlapping(1L, APRIL_21_10AM.plusMinutes(15), APRIL_21_10AM.plusMinutes(45))).isTrue();
        // Empieza justo cuando termina la otra: no hay traslape
        assertThat(appointments.existsOverlapping(1L, APRIL_21_10AM.plusMinutes(30), APRIL_21_10AM.plusMinutes(60))).isFalse();
        // 11:00–11:50 está ocupado por una cita cancelada, que libera el horario
        assertThat(appointments.existsOverlapping(1L, APRIL_21_10AM.plusHours(1), APRIL_21_10AM.plusMinutes(110))).isFalse();
        // Otro negocio no se ve afectado
        assertThat(appointments.existsOverlapping(2L, APRIL_21_10AM, APRIL_21_10AM.plusMinutes(30))).isFalse();
    }

    @Test
    void newAppointmentCopiesPriceAndComputesEnd() {
        User client = users.findByEmail("laura.sanchez@unam.mx").orElseThrow();
        ServiceOffering corteBarba = services.findById(2L).orElseThrow();
        LocalDateTime start = LocalDateTime.now().plusDays(30).withHour(10).withMinute(0).withSecond(0).withNano(0);

        Long id = appointments.save(new Appointment(client, corteBarba, start, "Sin prisa")).getId();
        corteBarba.setPrice(new BigDecimal("999.00"));
        em.flush();
        em.clear();

        Appointment saved = appointments.findByIdAndClientId(id, client.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(saved.getEndTime()).isEqualTo(start.plusMinutes(50));
        assertThat(saved.getPriceAtBooking()).isEqualByComparingTo("180.00");
        assertThat(appointments.findByIdAndServiceBusinessId(id, 1L)).isPresent();
        assertThat(appointments.findByIdAndServiceBusinessId(id, 2L)).isEmpty();
    }

    @Test
    void expiredPendingAppointmentsAreCancelledBySystem() {
        User client = users.findByEmail("miguel.flores@unam.mx").orElseThrow();
        ServiceOffering corte = services.findById(1L).orElseThrow();
        LocalDateTime now = LocalDateTime.now();
        Long pastId = appointments.save(new Appointment(client, corte, now.minusDays(2), null)).getId();
        Long futureId = appointments.save(new Appointment(client, corte, now.plusDays(2), null)).getId();
        CancellationReason systemReason = reasons.findByCancelledByOrderByName(CancelledBy.SYSTEM).getFirst();
        em.flush();

        assertThat(appointments.cancelExpiredPending(now, systemReason)).isGreaterThanOrEqualTo(1);
        em.clear();

        Appointment past = appointments.findById(pastId).orElseThrow();
        assertThat(past.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(past.getCancelledReason().getCancelledBy()).isEqualTo(CancelledBy.SYSTEM);
        assertThat(appointments.findById(futureId).orElseThrow().getStatus()).isEqualTo(AppointmentStatus.PENDING);
    }

    @Test
    void deactivationCancelsOnlyFutureActiveAppointments() {
        LocalDateTime now = LocalDateTime.now();
        long future = appointments.countFutureActiveByBusiness(3L, now);
        CancellationReason ownerReason = reasons.findByName("Cancelada por el dueño del negocio").orElseThrow();

        assertThat(appointments.cancelFutureActiveByBusiness(3L, now, ownerReason)).isEqualTo((int) future);
        assertThat(appointments.countFutureActiveByBusiness(3L, now)).isZero();
        // El historial (abril) no se toca
        em.clear();
        assertThat(appointments.findActiveByBusinessBetween(3L,
                LocalDateTime.of(2026, 4, 1, 0, 0), LocalDateTime.of(2026, 5, 1, 0, 0))).hasSize(1);
    }
}
