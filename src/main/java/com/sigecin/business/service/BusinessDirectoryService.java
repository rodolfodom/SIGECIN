package com.sigecin.business.service;

import com.sigecin.business.dto.BusinessDetail;
import com.sigecin.business.dto.DaySchedule;
import com.sigecin.business.entity.Business;
import com.sigecin.business.entity.BusinessCategory;
import com.sigecin.business.repository.BusinessCategoryRepository;
import com.sigecin.business.repository.BusinessRepository;
import com.sigecin.common.exception.NotFoundException;
import com.sigecin.serviceoffering.enums.ServiceStatus;
import com.sigecin.serviceoffering.repository.ServiceOfferingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Catálogo público de negocios. Solo expone negocios visibles (activos, con al menos un
 * servicio activo y un día de horario activo); los demás no existen para el cliente (404).
 */
@Service
@RequiredArgsConstructor
public class BusinessDirectoryService {

    public static final int DEFAULT_PAGE_SIZE = 12;
    public static final int MAX_PAGE_SIZE = 48;

    private final BusinessRepository businesses;
    private final BusinessCategoryRepository categories;
    private final ServiceOfferingRepository services;

    /** Búsqueda por nombre (parcial, sin distinguir mayúsculas) y/o categoría. */
    @Transactional(readOnly = true)
    public Page<Business> search(String name, Integer categoryId, int page, int size) {
        String term = name == null || name.isBlank() ? null : escapeLike(name.trim());
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        return businesses.searchVisible(term, categoryId, PageRequest.of(Math.max(page, 0), safeSize));
    }

    @Transactional(readOnly = true)
    public BusinessDetail getVisible(Long businessId) {
        Business business = businesses.findVisibleById(businessId)
                .orElseThrow(() -> new NotFoundException("Negocio " + businessId + " no disponible"));
        return new BusinessDetail(business, DaySchedule.weekOf(business),
                services.findByBusinessIdAndStatusOrderByName(businessId, ServiceStatus.ACTIVE));
    }

    @Transactional(readOnly = true)
    public List<BusinessCategory> categories() {
        return categories.findAllByOrderByIdAsc();
    }

    // "!" es el carácter de escape declarado en la consulta (BusinessRepository.SEARCH_FILTERS)
    static String escapeLike(String text) {
        return text.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
