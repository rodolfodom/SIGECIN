package com.sigecin.serviceoffering.entity;

import com.sigecin.business.entity.Business;
import com.sigecin.serviceoffering.enums.ServiceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Servicio ofrecido por un negocio (tabla service). Se llama ServiceOffering
 * para no confundirse con @Service ni con la capa de servicios.
 * No se borra físicamente porque las citas lo referencian: se desactiva.
 */
@Entity
@Table(name = "service")
public class ServiceOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(nullable = false, length = 120)
    private String name;

    private String description;

    @Column(name = "duration_min", nullable = false)
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer durationMin;

    // Precio vigente; las citas guardan su propia copia en price_at_booking
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "status_id", nullable = false)
    private ServiceStatus status = ServiceStatus.ACTIVE;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected ServiceOffering() {
    }

    public ServiceOffering(Business business, String name, Integer durationMin, BigDecimal price) {
        this.business = business;
        this.name = name;
        this.durationMin = durationMin;
        this.price = price;
    }

    public boolean isActive() {
        return status == ServiceStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public Business getBusiness() {
        return business;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDurationMin() {
        return durationMin;
    }

    public void setDurationMin(Integer durationMin) {
        this.durationMin = durationMin;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public ServiceStatus getStatus() {
        return status;
    }

    public void setStatus(ServiceStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
