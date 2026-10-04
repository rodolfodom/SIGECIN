package com.sigecin.appointment.repository;

import com.sigecin.appointment.entity.Appointment;
import com.sigecin.appointment.entity.CancellationReason;
import com.sigecin.appointment.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * (BusinessRepository.findByIdForUpdate) en la misma transacción y esta usa
     * READ COMMITTED: con REPEATABLE READ la consulta leería la foto tomada al inicio
     * de la transacción y no vería la cita que otra reserva acaba de confirmar.
     */
    @Query("""
            select count(a) > 0 from Appointment a
             where a.service.business.id = :businessId
               and a.status <> com.sigecin.appointment.enums.AppointmentStatus.CANCELLED
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
               and a.status <> com.sigecin.appointment.enums.AppointmentStatus.CANCELLED
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
               and a.status <> com.sigecin.appointment.enums.AppointmentStatus.CANCELLED
               and a.startTime > :now
            """)
    long countFutureActiveByBusiness(@Param("businessId") Long businessId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("""
            update Appointment a
               set a.status = com.sigecin.appointment.enums.AppointmentStatus.CANCELLED,
                   a.cancelledReason = :reason
             where a.service.id in (select s.id from ServiceOffering s where s.business.id = :businessId)
               and a.status <> com.sigecin.appointment.enums.AppointmentStatus.CANCELLED
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
               set a.status = com.sigecin.appointment.enums.AppointmentStatus.CANCELLED,
                   a.cancelledReason = :reason
             where a.status = com.sigecin.appointment.enums.AppointmentStatus.PENDING
               and a.startTime <= :now
            """)
    int cancelExpiredPending(@Param("now") LocalDateTime now, @Param("reason") CancellationReason reason);


    /** Citas del cliente con filtros opcionales, de la más reciente a la más antigua. */
    @Query(value = """
            select a from Appointment a
              join fetch a.service s
              join fetch s.business
              left join fetch a.cancelledReason
             where a.client.id = :clientId
               and (:status is null or a.status = :status)
               and (:from is null or a.startTime >= :from)
               and (:to is null or a.startTime < :to)
             order by a.startTime desc
            """,
            countQuery = """
            select count(a) from Appointment a
             where a.client.id = :clientId
               and (:status is null or a.status = :status)
               and (:from is null or a.startTime >= :from)
               and (:to is null or a.startTime < :to)
            """)
    Page<Appointment> findForClient(@Param("clientId") Long clientId,
                                    @Param("status") AppointmentStatus status,
                                    @Param("from") LocalDateTime from,
                                    @Param("to") LocalDateTime to,
                                    Pageable pageable);

    /** Cita con cliente, servicio, negocio y motivo cargados (para las páginas de detalle). */
    @Query("""
            select a from Appointment a
              join fetch a.client
              join fetch a.service s
              join fetch s.business
              left join fetch a.cancelledReason
             where a.id = :id
            """)
    Optional<Appointment> findDetailedById(@Param("id") Long id);

    /**
     * PENDING → CONFIRMED, solo si la cita sigue pendiente y no ha empezado. La condición
     * en el UPDATE evita pisar una cancelación simultánea (del cliente o de la tarea programada).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Appointment a
               set a.status = com.sigecin.appointment.enums.AppointmentStatus.CONFIRMED
             where a.id = :id
               and a.status = com.sigecin.appointment.enums.AppointmentStatus.PENDING
               and a.startTime > :now
            """)
    int confirmIfPending(@Param("id") Long id, @Param("now") LocalDateTime now);

    /** PENDING o CONFIRMED → CANCELLED, solo si la cita no ha empezado. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Appointment a
               set a.status = com.sigecin.appointment.enums.AppointmentStatus.CANCELLED,
                   a.cancelledReason = :reason
             where a.id = :id
               and a.status <> com.sigecin.appointment.enums.AppointmentStatus.CANCELLED
               and a.startTime > :now
            """)
    int cancelIfActive(@Param("id") Long id, @Param("reason") CancellationReason reason,
                       @Param("now") LocalDateTime now);
}
