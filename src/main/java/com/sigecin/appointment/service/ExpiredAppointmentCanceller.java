package com.sigecin.appointment.service;

import com.sigecin.appointment.entity.CancellationReason;
import com.sigecin.appointment.repository.AppointmentRepository;
import com.sigecin.appointment.repository.CancellationReasonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Tarea programada (al arrancar y cada 5 minutos): cancela las citas PENDING cuya hora
 * de inicio ya pasó sin que el negocio las confirmara. El UPDATE está condicionado a
 * status = PENDING, así que no pisa una confirmación simultánea. Supone una sola
 * instancia de la aplicación (monolito).
 */
@Service
public class ExpiredAppointmentCanceller {

    private static final Logger log = LoggerFactory.getLogger(ExpiredAppointmentCanceller.class);

    private final AppointmentRepository appointments;
    private final CancellationReasonRepository reasons;

    public ExpiredAppointmentCanceller(AppointmentRepository appointments, CancellationReasonRepository reasons) {
        this.appointments = appointments;
        this.reasons = reasons;
    }

    @Scheduled(initialDelay = 0, fixedDelayString = "PT5M")
    @Transactional
    public void cancelExpiredPending() {
        CancellationReason reason = reasons.findByName(CancellationReason.NOT_CONFIRMED_IN_TIME)
                .orElseThrow(() -> new IllegalStateException("Falta el motivo de cancelación automática"));
        int cancelled = appointments.cancelExpiredPending(LocalDateTime.now(), reason);
        if (cancelled > 0) {
            log.info("Citas pendientes vencidas canceladas automáticamente: {}", cancelled);
        }
    }
}
