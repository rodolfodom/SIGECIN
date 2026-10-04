package com.sigecin.support;

import com.sigecin.appointment.entity.Appointment;
import com.sigecin.appointment.repository.AppointmentRepository;
import com.sigecin.serviceoffering.entity.ServiceOffering;
import com.sigecin.user.entity.User;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

/** Citas de prueba en fechas lejos de los datos del script (que están en el mes en curso). */
@Component
public class AppointmentTestData {

    // Datos de ddl_sigecin.sql
    public static final long CARLOS = 1L;
    public static final long LAURA = 2L;
    public static final long CORTE_CLASICO = 1L;      // Barbería, 30 min, $120
    public static final long CORTE_BARBA = 2L;        // Barbería, 50 min, $180
    public static final long MASAJE = 4L;             // Spa, 60 min, $450
    public static final long MANICURE_INACTIVO = 6L;  // Spa, inactivo

    private final AppointmentRepository appointments;
    private final EntityManager em;

    public AppointmentTestData(AppointmentRepository appointments, EntityManager em) {
        this.appointments = appointments;
        this.em = em;
    }

    /** Un lunes dentro de dos meses: la Barbería abre de 09:00 a 19:00 y no hay citas del script. */
    public static LocalDate futureMonday() {
        return LocalDate.now().plusMonths(2).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
    }

    public Appointment create(long clientId, long serviceId, LocalDateTime start) {
        return appointments.save(new Appointment(em.find(User.class, clientId),
                em.find(ServiceOffering.class, serviceId), start, null));
    }
}
