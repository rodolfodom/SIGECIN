package com.sigecin.appointment.service;

import com.sigecin.appointment.dto.Availability;
import com.sigecin.appointment.entity.Appointment;
import com.sigecin.appointment.repository.AppointmentRepository;
import com.sigecin.business.dto.DaySchedule;
import com.sigecin.business.entity.Business;
import com.sigecin.business.repository.BusinessRepository;
import com.sigecin.common.exception.NotFoundException;
import com.sigecin.serviceoffering.entity.ServiceOffering;
import com.sigecin.serviceoffering.repository.ServiceOfferingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcula las horas de inicio libres de un servicio en una fecha. Las horas se ofrecen
 * cada {@value #SLOT_STEP_MINUTES} minutos a partir de la apertura; una hora es libre si
 * la cita completa cabe antes del cierre, aún no pasó y no se traslapa con otra cita no
 * cancelada del negocio. Es solo una sugerencia: la reserva vuelve a validarlo todo.
 */
@Service
public class AvailabilityService {

    public static final int SLOT_STEP_MINUTES = 15;

    private final BusinessRepository businesses;
    private final ServiceOfferingRepository services;
    private final AppointmentRepository appointments;

    public AvailabilityService(BusinessRepository businesses, ServiceOfferingRepository services,
                               AppointmentRepository appointments) {
        this.businesses = businesses;
        this.services = services;
        this.appointments = appointments;
    }

    /** Negocio no visible o servicio que no es suyo o está inactivo: 404. */
    @Transactional(readOnly = true)
    public Availability forDate(Long businessId, Long serviceId, LocalDate date) {
        Business business = businesses.findVisibleById(businessId)
                .orElseThrow(() -> new NotFoundException("Negocio " + businessId + " no disponible"));
        ServiceOffering service = services.findByIdAndBusinessId(serviceId, businessId)
                .filter(ServiceOffering::isActive)
                .orElseThrow(() -> new NotFoundException("Servicio " + serviceId + " no disponible"));

        DaySchedule day = DaySchedule.weekOf(business).get(date.getDayOfWeek().getValue() - 1);
        if (!day.open()) {
            return new Availability(business, service, date, day, List.of());
        }
        List<Appointment> taken = appointments.findActiveByBusinessBetween(
                businessId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        LocalDateTime now = LocalDateTime.now();
        List<LocalTime> slots = new ArrayList<>();
        LocalDateTime start = date.atTime(day.openTime());
        LocalDateTime close = date.atTime(day.closeTime());
        while (!start.plusMinutes(service.getDurationMin()).isAfter(close)) {
            LocalDateTime end = start.plusMinutes(service.getDurationMin());
            LocalDateTime candidate = start;
            boolean free = taken.stream().noneMatch(a -> a.getStartTime().isBefore(end) && a.getEndTime().isAfter(candidate));
            if (candidate.isAfter(now) && free) {
                slots.add(candidate.toLocalTime());
            }
            start = start.plusMinutes(SLOT_STEP_MINUTES);
        }
        return new Availability(business, service, date, day, slots);
    }
}
