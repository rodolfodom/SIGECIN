package com.sigecin.appointment.service;

import com.sigecin.appointment.entity.Appointment;
import com.sigecin.appointment.exception.BookingRejectedException;
import com.sigecin.appointment.repository.AppointmentRepository;
import com.sigecin.business.entity.Business;
import com.sigecin.business.entity.BusinessSchedule;
import com.sigecin.business.repository.BusinessRepository;
import com.sigecin.business.repository.BusinessScheduleRepository;
import com.sigecin.serviceoffering.entity.ServiceOffering;
import com.sigecin.serviceoffering.repository.ServiceOfferingRepository;
import com.sigecin.user.entity.User;
import com.sigecin.user.enums.Role;
import com.sigecin.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * Registro de citas. Dos reservas simultáneas para el mismo negocio se serializan
 * bloqueando la fila del negocio (SELECT ... FOR UPDATE) antes de verificar traslapes.
 */
@Service
public class BookingService {

    private final ServiceOfferingRepository services;
    private final BusinessRepository businesses;
    private final BusinessScheduleRepository schedules;
    private final AppointmentRepository appointments;
    private final UserRepository users;

    public BookingService(ServiceOfferingRepository services, BusinessRepository businesses,
                          BusinessScheduleRepository schedules, AppointmentRepository appointments,
                          UserRepository users) {
        this.services = services;
        this.businesses = businesses;
        this.schedules = schedules;
        this.appointments = appointments;
        this.users = users;
    }

    /**
     * Crea la cita en PENDING con el precio vigente del servicio, o lanza
     * {@link BookingRejectedException} con el motivo.
     * <p>
     * READ COMMITTED es indispensable: con REPEATABLE READ (el nivel por defecto de InnoDB)
     * la consulta de traslapes leería la foto tomada en la primera lectura de la transacción,
     * anterior a la espera por el bloqueo, y no vería la cita que la reserva que tenía el
     * bloqueo acaba de confirmar. El resultado sería una reserva duplicada.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Appointment book(Long clientId, Long serviceId, LocalDateTime requestedStart, String clientNotes) {
        ServiceOffering service = services.findById(serviceId)
                .orElseThrow(() -> new BookingRejectedException(BookingRejectedException.UNAVAILABLE));
        Business business = businesses.findByIdForUpdate(service.getBusiness().getId())
                .orElseThrow(() -> new BookingRejectedException(BookingRejectedException.UNAVAILABLE));
        if (!service.isActive() || !business.isActive()) {
            throw new BookingRejectedException(BookingRejectedException.UNAVAILABLE);
        }

        LocalDateTime start = requestedStart.truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime end = start.plusMinutes(service.getDurationMin());
        if (!start.isAfter(LocalDateTime.now())) {
            throw new BookingRejectedException(BookingRejectedException.PAST);
        }
        if (!fitsSchedule(business.getId(), start, end)) {
            throw new BookingRejectedException(BookingRejectedException.OUTSIDE_HOURS);
        }
        if (appointments.existsOverlapping(business.getId(), start, end)) {
            throw new BookingRejectedException(BookingRejectedException.TAKEN);
        }

        User client = users.findById(clientId)
                .filter(user -> user.getRole() == Role.CLIENT)
                .orElseThrow(() -> new IllegalStateException("Solo un usuario CLIENT puede reservar"));
        return appointments.save(new Appointment(client, service, start, clientNotes));
    }

    /** La cita completa cabe en el horario activo de ese día (sin pasar de la medianoche). */
    private boolean fitsSchedule(Long businessId, LocalDateTime start, LocalDateTime end) {
        if (!end.toLocalDate().equals(start.toLocalDate()) && !end.toLocalTime().equals(LocalTime.MIDNIGHT)) {
            return false;
        }
        BusinessSchedule day = schedules
                .findByBusinessIdAndDayOfWeekAndActiveTrue(businessId, start.getDayOfWeek())
                .orElse(null);
        return day != null
                && !start.toLocalTime().isBefore(day.getOpenTime())
                && !end.isAfter(start.toLocalDate().atTime(day.getCloseTime()));
    }
}
