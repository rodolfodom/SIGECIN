package com.sigecin.appointment.repository;

import com.sigecin.appointment.dto.AppointmentDetailRow;
import com.sigecin.appointment.enums.AppointmentStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lectura de la vista v_appointment_detail con JdbcClient: las vistas no son entidades
 * JPA (son de solo lectura y sin llave natural) y no reciben parámetros, así que el
 * filtro por negocio y fechas va en el WHERE.
 */
@Repository
public class AppointmentDetailRepository {

    private final JdbcClient jdbc;

    public AppointmentDetailRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Citas del negocio que empiezan en [from, to), opcionalmente de un estado, por hora de inicio. */
    public List<AppointmentDetailRow> findForBusiness(Long businessId, LocalDateTime from, LocalDateTime to,
                                                      AppointmentStatus status) {
        return jdbc.sql("""
                        select * from v_appointment_detail
                         where business_id = :businessId
                           and start_time >= :from
                           and start_time < :to
                           and (:status is null or status = :status)
                         order by start_time, appointment_id
                        """)
                .param("businessId", businessId)
                .param("from", from)
                .param("to", to)
                .param("status", status == null ? null : status.name())
                .query(AppointmentDetailRow.class)
                .list();
    }

    /** Próximas citas no canceladas de hoy (desde {@code now} hasta medianoche), las más inmediatas primero. */
    public List<AppointmentDetailRow> findUpcomingToday(Long businessId, LocalDateTime now, int limit) {
        return jdbc.sql("""
                        select * from v_appointment_detail
                         where business_id = :businessId
                           and status <> 'CANCELLED'
                           and start_time >= :now
                           and start_time < :tomorrow
                         order by start_time, appointment_id
                         limit :limit
                        """)
                .param("businessId", businessId)
                .param("now", now)
                .param("tomorrow", now.toLocalDate().plusDays(1).atStartOfDay())
                .param("limit", limit)
                .query(AppointmentDetailRow.class)
                .list();
    }
}
