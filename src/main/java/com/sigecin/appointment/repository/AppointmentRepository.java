package com.sigecin.appointment.repository;

import com.sigecin.appointment.Appointment;
import com.sigecin.appointment.CancellationReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /**
     * ¿Hay alguna cita no cancelada del negocio que se traslape con [start, end)?
     * Solo es segura ante concurrencia si antes se bloqueó la fila del negocio
     * (BusinessRepository.findByIdForUpdate) en la misma transacción.
     */
    @Query("""
            select count(a) > 0 from Appointment a
             where a.service.business.id = :businessId
               and a.status <> com.sigecin.appointment.AppointmentStatus.CANCELLED
               and a.startTime < :end
               and a.endTime > :start
            """)
    boolean existsOverlapping(@Param("businessId") Long businessId,
                              @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end);

    /** Citas no canceladas del negocio en un rango, para calcular la disponibilidad. */
    @Query("""
            select a from Appointment a
             where a.service.business.id = :businessId
               and a.status <> com.sigecin.appointment.AppointmentStatus.CANCELLED
               and a.startTime < :to
               and a.endTime > :from
             order by a.startTime
            """)
    List<Appointment> findActiveByBusinessBetween(@Param("businessId") Long businessId,
                                                  @Param("from") LocalDateTime from,
                                                  @Param("to") LocalDateTime to);

    /** Cita del cliente (si pertenece a otro, vacío → 404). */
    Optional<Appointment> findByIdAndClientId(Long id, Long clientId);

    /** Cita de un negocio (si pertenece a otro, vacío → 404). */
    Optional<Appointment> findByIdAndServiceBusinessId(Long id, Long businessId);

    /** Citas futuras PENDING o CONFIRMED del negocio; se cancelan al darlo de baja. */
    @Query("""
            select count(a) from Appointment a
             where a.service.business.id = :businessId
               and a.status <> com.sigecin.appointment.AppointmentStatus.CANCELLED
               and a.startTime > :now
            """)
    long countFutureActiveByBusiness(@Param("businessId") Long businessId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("""
            update Appointment a
               set a.status = com.sigecin.appointment.AppointmentStatus.CANCELLED,
                   a.cancelledReason = :reason
             where a.service.id in (select s.id from ServiceOffering s where s.business.id = :businessId)
               and a.status <> com.sigecin.appointment.AppointmentStatus.CANCELLED
               and a.startTime > :now
            """)
    int cancelFutureActiveByBusiness(@Param("businessId") Long businessId,
                                     @Param("now") LocalDateTime now,
                                     @Param("reason") CancellationReason reason);

    /**
     * Cancela las citas PENDING cuya hora de inicio ya pasó. La condición sobre
     * el estado evita pisar una confirmación concurrente.
     */
    @Modifying
    @Query("""
            update Appointment a
               set a.status = com.sigecin.appointment.AppointmentStatus.CANCELLED,
                   a.cancelledReason = :reason
             where a.status = com.sigecin.appointment.AppointmentStatus.PENDING
               and a.startTime <= :now
            """)
    int cancelExpiredPending(@Param("now") LocalDateTime now, @Param("reason") CancellationReason reason);

}
