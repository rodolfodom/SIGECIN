package com.sigecin.appointment.service;

import com.sigecin.IntegrationTest;
import com.sigecin.appointment.entity.Appointment;
import com.sigecin.appointment.entity.CancellationReason;
import com.sigecin.appointment.enums.AppointmentStatus;
import com.sigecin.appointment.repository.AppointmentRepository;
import com.sigecin.support.AppointmentTestData;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.sigecin.support.AppointmentTestData.CARLOS;
import static com.sigecin.support.AppointmentTestData.CORTE_CLASICO;
import static org.assertj.core.api.Assertions.assertThat;

/** La tarea programada cancela solo las citas PENDING que ya llegaron a su hora. */
@IntegrationTest
@Import(AppointmentTestData.class)
@Transactional
class ExpiredAppointmentCancellerTests {

    @Autowired ExpiredAppointmentCanceller canceller;
    @Autowired AppointmentTestData data;
    @Autowired AppointmentRepository appointments;
    @Autowired EntityManager em;

    @Test
    void cancelsOnlyExpiredPendingAppointments() {
        Appointment expired = data.create(CARLOS, CORTE_CLASICO, LocalDateTime.now().minusMinutes(5));
        Appointment confirmedPast = data.create(CARLOS, CORTE_CLASICO, LocalDateTime.now().minusDays(1));
        confirmedPast.setStatus(AppointmentStatus.CONFIRMED);
        Appointment future = data.create(CARLOS, CORTE_CLASICO, LocalDateTime.now().plusDays(1));
        em.flush();

        canceller.cancelExpiredPending();
        em.clear();

        Appointment cancelled = appointments.findDetailedById(expired.getId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(cancelled.getCancelledReason().getName()).isEqualTo(CancellationReason.NOT_CONFIRMED_IN_TIME);
        assertThat(appointments.findById(confirmedPast.getId()).orElseThrow().getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(appointments.findById(future.getId()).orElseThrow().getStatus()).isEqualTo(AppointmentStatus.PENDING);
    }
}
