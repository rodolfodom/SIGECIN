package com.sigecin.appointment.repository;

import com.sigecin.IntegrationTest;
import com.sigecin.appointment.entity.Appointment;
import com.sigecin.appointment.entity.CancellationReason;
import com.sigecin.appointment.enums.AppointmentStatus;
import com.sigecin.appointment.enums.CancelledBy;
import com.sigecin.appointment.repository.AppointmentRepository;
import com.sigecin.appointment.repository.CancellationReasonRepository;
import com.sigecin.serviceoffering.entity.ServiceOffering;
import com.sigecin.serviceoffering.repository.ServiceOfferingRepository;
import com.sigecin.user.entity.User;
import com.sigecin.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Citas: traslapes, precio congelado, cancelaciones masivas y motivos por actor.
 * Usa el esquema y los datos de prueba de ddl_sigecin.sql; cada prueba se revierte.
 */
@IntegrationTest
@Transactional
class AppointmentRepositoryTests {

    // Cita histórica del script: Barbería (negocio 1), 2026-04-21 10:00–10:30, CONFIRMED
    private static final LocalDateTime APRIL_21_10AM = LocalDateTime.of(2026, 4, 21, 10, 0);

    @Autowired UserRepository users;
    @Autowired ServiceOfferingRepository services;
    @Autowired AppointmentRepository appointments;
    @Autowired CancellationReasonRepository reasons;
    @Autowired EntityManager em;

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
        CancellationReason systemReason = reasons.findByCancelledByOrderByName(CancelledBy.SYSTEM).get(0);
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
