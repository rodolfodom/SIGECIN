package com.sigecin.serviceoffering.repository;

import com.sigecin.serviceoffering.entity.ServiceOffering;
import com.sigecin.serviceoffering.enums.ServiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, Long> {

    /** Todos los servicios del negocio (activos e inactivos), para su administración. */
    List<ServiceOffering> findByBusinessIdOrderByName(Long businessId);

    List<ServiceOffering> findByBusinessIdAndStatusOrderByName(Long businessId, ServiceStatus status);

    /** Busca un servicio verificando que pertenezca al negocio (si no, 404). */
    Optional<ServiceOffering> findByIdAndBusinessId(Long id, Long businessId);
}
