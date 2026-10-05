package com.sigecin.business.entity;

import com.sigecin.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "business")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Propietario con rol BUSINESS; cada usuario tiene un solo negocio
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    @Setter
    private BusinessCategory category;

    @Column(nullable = false, length = 150)
    @Setter
    private String name;

    @Column(columnDefinition = "text")
    @Setter
    private String description;

    @Column(length = 20)
    @Setter
    private String phone;

    @Setter
    private String address;

    // Baja lógica: un negocio inactivo no acepta reservas y conserva su historial
    @Column(nullable = false)
    @Setter
    private boolean active = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "business", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayOfWeek")
    private List<BusinessSchedule> schedules = new ArrayList<>();

    public Business(User owner, BusinessCategory category, String name) {
        this.owner = owner;
        this.category = category;
        this.name = name;
    }
}
