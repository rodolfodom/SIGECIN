package com.sigecin.api.web;

import com.sigecin.api.dto.AvailabilityResponse;
import com.sigecin.api.dto.BusinessDetailResponse;
import com.sigecin.api.dto.BusinessSummary;
import com.sigecin.api.dto.PageResponse;
import com.sigecin.appointment.service.AvailabilityService;
import com.sigecin.business.service.BusinessDirectoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Catálogo público de negocios (no requiere token). Solo expone negocios visibles. */
@RestController
@RequestMapping("/api/businesses")
@RequiredArgsConstructor
public class BusinessApiController {

    private final BusinessDirectoryService directory;
    private final AvailabilityService availabilityService;

    @GetMapping
    public PageResponse<BusinessSummary> search(@RequestParam(required = false) String name,
                                                @RequestParam(required = false) Integer categoryId,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "" + BusinessDirectoryService.DEFAULT_PAGE_SIZE) int size) {
        return PageResponse.of(directory.search(name, categoryId, page, size), BusinessSummary::of);
    }

    @GetMapping("/{id}")
    public BusinessDetailResponse detail(@PathVariable Long id) {
        return BusinessDetailResponse.of(directory.getVisible(id));
    }

    @GetMapping("/{id}/availability")
    public AvailabilityResponse availability(@PathVariable Long id, @RequestParam Long serviceId,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return AvailabilityResponse.of(availabilityService.forDate(id, serviceId, date));
    }
}
