package com.sigecin.serviceoffering.service;

import com.sigecin.business.entity.Business;
import com.sigecin.business.service.BusinessService;
import com.sigecin.common.exception.NotFoundException;
import com.sigecin.serviceoffering.dto.ServiceData;
import com.sigecin.serviceoffering.entity.ServiceOffering;
import com.sigecin.serviceoffering.enums.ServiceStatus;
import com.sigecin.serviceoffering.repository.ServiceOfferingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicios del negocio del dueño. Un cambio de precio o duración solo afecta a las
 * citas nuevas (las existentes guardan su precio y su hora de fin). Desactivar un
 * servicio solo impide nuevas reservas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceOfferingService {

    private final ServiceOfferingRepository services;
    private final BusinessService businessService;

    @Transactional(readOnly = true)
    public List<ServiceOffering> listOwned(Long ownerId) {
        return services.findByBusinessIdOrderByName(businessService.getByOwner(ownerId).getId());
    }

    /** Servicio del negocio del dueño; si es de otro negocio, 404. */
    @Transactional(readOnly = true)
    public ServiceOffering getOwned(Long ownerId, Long serviceId) {
        Long businessId = businessService.getByOwner(ownerId).getId();
        return services.findByIdAndBusinessId(serviceId, businessId)
                .orElseThrow(() -> new NotFoundException("Servicio " + serviceId + " no encontrado"));
    }

    @Transactional
    public ServiceOffering create(Long ownerId, ServiceData data) {
        Business business = businessService.getByOwner(ownerId);
        ServiceOffering service = new ServiceOffering(business, data.name(), data.durationMin(), data.price());
        service.setDescription(data.description());
        ServiceOffering saved = services.save(service);
        log.info("Servicio {} creado en el negocio {}", saved.getId(), business.getId());
        return saved;
    }

    @Transactional
    public void update(Long ownerId, Long serviceId, ServiceData data) {
        ServiceOffering service = getOwned(ownerId, serviceId);
        service.setName(data.name());
        service.setDescription(data.description());
        service.setDurationMin(data.durationMin());
        service.setPrice(data.price());
        services.save(service);
    }

    /** Activa o desactiva el servicio; devuelve el estado resultante. */
    @Transactional
    public ServiceStatus toggleStatus(Long ownerId, Long serviceId) {
        ServiceOffering service = getOwned(ownerId, serviceId);
        service.setStatus(service.isActive() ? ServiceStatus.INACTIVE : ServiceStatus.ACTIVE);
        services.save(service);
        log.info("Servicio {} cambiado a {}", serviceId, service.getStatus());
        return service.getStatus();
    }
}
