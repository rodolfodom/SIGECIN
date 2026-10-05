package com.sigecin.business.service;

import com.sigecin.appointment.entity.CancellationReason;
import com.sigecin.appointment.repository.AppointmentRepository;
import com.sigecin.appointment.repository.CancellationReasonRepository;
import com.sigecin.business.dto.BusinessData;
import com.sigecin.business.dto.Visibility;
import com.sigecin.business.entity.Business;
import com.sigecin.business.entity.BusinessCategory;
import com.sigecin.business.exception.BusinessAlreadyExistsException;
import com.sigecin.business.repository.BusinessCategoryRepository;
import com.sigecin.business.repository.BusinessRepository;
import com.sigecin.business.repository.BusinessScheduleRepository;
import com.sigecin.common.exception.NotFoundException;
import com.sigecin.serviceoffering.enums.ServiceStatus;
import com.sigecin.serviceoffering.repository.ServiceOfferingRepository;
import com.sigecin.user.entity.User;
import com.sigecin.user.enums.Role;
import com.sigecin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Alta, modificación, baja y reactivación del negocio de un dueño (rol BUSINESS). */
@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessService {

    private final BusinessRepository businesses;
    private final BusinessCategoryRepository categories;
    private final BusinessScheduleRepository schedules;
    private final ServiceOfferingRepository services;
    private final AppointmentRepository appointments;
    private final CancellationReasonRepository reasons;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public boolean hasBusiness(Long ownerId) {
        return businesses.existsByOwnerId(ownerId);
    }

    /** Negocio del dueño con su categoría y horario cargados. */
    @Transactional(readOnly = true)
    public Business getByOwner(Long ownerId) {
        return businesses.findByOwnerId(ownerId)
                .orElseThrow(() -> new NotFoundException("El usuario " + ownerId + " no tiene negocio"));
    }

    @Transactional(readOnly = true)
    public List<BusinessCategory> categories() {
        return categories.findAllByOrderByIdAsc();
    }

    @Transactional
    public Business create(Long ownerId, BusinessData data) {
        User owner = users.findById(ownerId)
                .filter(user -> user.getRole() == Role.BUSINESS)
                .orElseThrow(() -> new IllegalStateException("Solo un usuario BUSINESS puede registrar un negocio"));
        if (businesses.existsByOwnerId(ownerId)) {
            throw new BusinessAlreadyExistsException();
        }
        Business business = new Business(owner, category(data.categoryId()), data.name());
        apply(business, data);
        try {
            Business saved = businesses.saveAndFlush(business);
            log.info("Negocio {} registrado por el usuario {}", saved.getId(), ownerId);
            return saved;
        } catch (DataIntegrityViolationException e) {
            // Doble envío del formulario: uq_business_user ya tiene el negocio
            throw new BusinessAlreadyExistsException();
        }
    }

    @Transactional
    public void update(Long ownerId, BusinessData data) {
        Business business = getByOwner(ownerId);
        business.setCategory(category(data.categoryId()));
        business.setName(data.name());
        apply(business, data);
        businesses.save(business);
    }

    @Transactional(readOnly = true)
    public Visibility visibility(Business business) {
        return new Visibility(
                business.isActive(),
                services.existsByBusinessIdAndStatus(business.getId(), ServiceStatus.ACTIVE),
                schedules.existsByBusinessIdAndActiveTrue(business.getId()));
    }

    /** Citas futuras PENDING o CONFIRMED que se cancelarían al dar de baja el negocio. */
    @Transactional(readOnly = true)
    public long countAppointmentsCancelledOnDeactivation(Long ownerId) {
        return appointments.countFutureActiveByBusiness(getByOwner(ownerId).getId(), LocalDateTime.now());
    }

    /**
     * Baja lógica: el negocio queda inactivo y, en la misma transacción, se cancelan sus
     * citas futuras con el motivo "Cancelada por el dueño del negocio". Devuelve cuántas.
     */
    @Transactional
    public int deactivate(Long ownerId) {
        Business business = getByOwner(ownerId);
        if (!business.isActive()) {
            return 0;
        }
        business.setActive(false);
        CancellationReason reason = reasons.findByName(CancellationReason.BUSINESS_DEACTIVATED)
                .orElseThrow(() -> new IllegalStateException("Falta el motivo de cancelación por baja del negocio"));
        int cancelled = appointments.cancelFutureActiveByBusiness(business.getId(), LocalDateTime.now(), reason);
        log.info("Negocio {} dado de baja; citas futuras canceladas: {}", business.getId(), cancelled);
        return cancelled;
    }

    /** Reactiva el negocio; las citas canceladas en la baja no se restauran. */
    @Transactional
    public void activate(Long ownerId) {
        Business business = getByOwner(ownerId);
        business.setActive(true);
        log.info("Negocio {} reactivado", business.getId());
    }

    private BusinessCategory category(Integer categoryId) {
        return categories.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Categoría " + categoryId + " inexistente"));
    }

    private static void apply(Business business, BusinessData data) {
        business.setDescription(data.description());
        business.setPhone(data.phone());
        business.setAddress(data.address());
    }
}
