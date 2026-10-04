package com.sigecin.appointment.repository;

import com.sigecin.appointment.entity.CancellationReason;
import com.sigecin.appointment.enums.CancelledBy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CancellationReasonRepository extends JpaRepository<CancellationReason, Integer> {

    /** Motivos que puede usar un actor (para los formularios de cancelación). */
    List<CancellationReason> findByCancelledByOrderByName(CancelledBy cancelledBy);

    Optional<CancellationReason> findByIdAndCancelledBy(Integer id, CancelledBy cancelledBy);

    Optional<CancellationReason> findByName(String name);
}
