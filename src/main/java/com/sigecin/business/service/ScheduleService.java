package com.sigecin.business.service;

import com.sigecin.business.dto.DaySchedule;
import com.sigecin.business.entity.Business;
import com.sigecin.business.entity.BusinessSchedule;
import com.sigecin.business.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Horario semanal del negocio: un registro por día, que se desactiva en lugar de borrarse. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleService {

    private final BusinessService businessService;
    private final BusinessRepository businesses;

    /**
     * Guarda los días recibidos. Un día abierto crea o actualiza su horario; un día cerrado
     * desactiva el existente y conserva sus horas para cuando se vuelva a abrir.
     */
    @Transactional
    public void saveWeek(Long ownerId, List<DaySchedule> week) {
        Business business = businessService.getByOwner(ownerId);
        for (DaySchedule day : week) {
            BusinessSchedule existing = business.getSchedules().stream()
                    .filter(s -> s.getDayOfWeek() == day.day())
                    .findFirst()
                    .orElse(null);
            if (day.open()) {
                if (!day.closeTime().isAfter(day.openTime())) {
                    throw new IllegalArgumentException("El cierre debe ser posterior a la apertura: " + day.day());
                }
                if (existing == null) {
                    business.getSchedules().add(
                            new BusinessSchedule(business, day.day(), day.openTime(), day.closeTime()));
                } else {
                    existing.setOpenTime(day.openTime());
                    existing.setCloseTime(day.closeTime());
                    existing.setActive(true);
                }
            } else if (existing != null) {
                existing.setActive(false);
            }
        }
        businesses.save(business);
        log.info("Horario semanal actualizado del negocio {}", business.getId());
    }
}
