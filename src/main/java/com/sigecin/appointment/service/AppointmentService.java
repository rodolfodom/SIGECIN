package com.sigecin.appointment.service;

import com.sigecin.appointment.dto.AppointmentDetailRow;
import com.sigecin.appointment.entity.Appointment;
import com.sigecin.appointment.entity.CancellationReason;
import com.sigecin.appointment.enums.AppointmentStatus;
import com.sigecin.appointment.enums.CancelledBy;
import com.sigecin.appointment.exception.AppointmentNotModifiableException;
import com.sigecin.appointment.repository.AppointmentDetailRepository;
import com.sigecin.appointment.repository.AppointmentRepository;
import com.sigecin.appointment.repository.CancellationReasonRepository;
import com.sigecin.business.service.BusinessService;
import com.sigecin.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Consulta y cambios de estado de las citas, para el cliente y para el dueño del negocio.
 * Cada cambio de estado es un UPDATE condicionado al estado actual y a que la cita no haya
 * empezado, de modo que dos acciones simultáneas no se pisan: la segunda ya no encuentra
 * la cita en el estado esperado y se rechaza.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    public static final int PAGE_SIZE = 10;

    private final AppointmentRepository appointments;
    private final AppointmentDetailRepository details;
    private final CancellationReasonRepository reasons;
    private final BusinessService businessService;

    @Transactional(readOnly = true)
    public List<CancellationReason> reasonsFor(CancelledBy actor) {
        return reasons.findByCancelledByOrderByName(actor);
    }

    // --- Cliente ---

    /** Citas del cliente; {@code to} es inclusivo (todo ese día). */
    @Transactional(readOnly = true)
    public Page<Appointment> listForClient(Long clientId, AppointmentStatus status, LocalDate from, LocalDate to, int page) {
        return appointments.findForClient(clientId, status,
                from == null ? null : from.atStartOfDay(),
                to == null ? null : to.plusDays(1).atStartOfDay(),
                PageRequest.of(Math.max(page, 0), PAGE_SIZE));
    }

    @Transactional(readOnly = true)
    public Appointment getForClient(Long clientId, Long appointmentId) {
        return appointments.findDetailedById(appointmentId)
                .filter(a -> a.getClient().getId().equals(clientId))
                .orElseThrow(() -> notFound(appointmentId));
    }

    @Transactional
    public void cancelByClient(Long clientId, Long appointmentId, Integer reasonId) {
        getForClient(clientId, appointmentId);
        cancel(appointmentId, reasonId, CancelledBy.CLIENT);
    }

    // --- Dueño del negocio ---

    @Transactional(readOnly = true)
    public Appointment getForBusiness(Long ownerId, Long appointmentId) {
        Long businessId = businessService.getByOwner(ownerId).getId();
        return appointments.findDetailedById(appointmentId)
                .filter(a -> a.getService().getBusiness().getId().equals(businessId))
                .orElseThrow(() -> notFound(appointmentId));
    }

    /** Citas del negocio que empiezan en [from, to), para el calendario. */
    @Transactional(readOnly = true)
    public List<AppointmentDetailRow> calendar(Long ownerId, LocalDate from, LocalDate to, AppointmentStatus status) {
        Long businessId = businessService.getByOwner(ownerId).getId();
        return details.findForBusiness(businessId, from.atStartOfDay(), to.atStartOfDay(), status);
    }

    @Transactional
    public void confirm(Long ownerId, Long appointmentId) {
        getForBusiness(ownerId, appointmentId);
        if (appointments.confirmIfPending(appointmentId, LocalDateTime.now()) == 0) {
            throw new AppointmentNotModifiableException("appointment.error.not-confirmable");
        }
        log.info("Cita {} confirmada por el dueño {}", appointmentId, ownerId);
    }

    @Transactional
    public void cancelByBusiness(Long ownerId, Long appointmentId, Integer reasonId) {
        getForBusiness(ownerId, appointmentId);
        cancel(appointmentId, reasonId, CancelledBy.BUSINESS);
    }

    /** El motivo es obligatorio y debe ser del actor que cancela. */
    private void cancel(Long appointmentId, Integer reasonId, CancelledBy actor) {
        CancellationReason reason = reasonId == null ? null
                : reasons.findByIdAndCancelledBy(reasonId, actor).orElse(null);
        if (reason == null) {
            throw new AppointmentNotModifiableException("appointment.error.invalid-reason");
        }
        if (appointments.cancelIfActive(appointmentId, reason, LocalDateTime.now()) == 0) {
            throw new AppointmentNotModifiableException("appointment.error.not-cancellable");
        }
        log.info("Cita {} cancelada por {} (motivo: {})", appointmentId, actor, reason.getName());
    }

    private static NotFoundException notFound(Long appointmentId) {
        return new NotFoundException("Cita " + appointmentId + " no encontrada");
    }
}
