package com.sigecin.business.repository;

import com.sigecin.business.entity.BusinessSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface BusinessScheduleRepository extends JpaRepository<BusinessSchedule, Long> {

    List<BusinessSchedule> findByBusinessIdOrderByDayOfWeek(Long businessId);

    /** Horario activo de un día; vacío si el negocio no abre ese día. */
    Optional<BusinessSchedule> findByBusinessIdAndDayOfWeekAndActiveTrue(Long businessId, DayOfWeek dayOfWeek);
}
