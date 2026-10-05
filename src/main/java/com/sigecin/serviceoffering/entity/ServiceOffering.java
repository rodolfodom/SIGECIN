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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServiceOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(nullable = false, length = 120)
    @Setter
    private String name;

    @Setter
    private String description;

    @Column(name = "duration_min", nullable = false)
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Setter
    private Integer durationMin;

    // Precio vigente; las citas guardan su propia copia en price_at_booking
    @Column(nullable = false, precision = 10, scale = 2)
    @Setter
    private BigDecimal price;

    @Column(name = "status_id", nullable = false)
    @Setter
    private ServiceStatus status = ServiceStatus.ACTIVE;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public ServiceOffering(Business business, String name, Integer durationMin, BigDecimal price) {
        this.business = business;
        this.name = name;
        this.durationMin = durationMin;
        this.price = price;
    }

    public boolean isActive() {
        return status == ServiceStatus.ACTIVE;
    }
}
