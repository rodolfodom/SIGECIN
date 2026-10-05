package com.sigecin.user.entity;

import com.sigecin.business.entity.Business;
import com.sigecin.user.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "full_name", nullable = false, length = 120)
    @Setter
    private String fullName;

    @Column(nullable = false, length = 150, unique = true)
    @Setter
    private String email;

    // Hash bcrypt, nunca texto plano
    @Column(nullable = false)
    @Setter
    private String password;

    @Column(nullable = false)
    @Setter
    private boolean active = true;

    // Las fechas de auditoría las llena MariaDB (DEFAULT / ON UPDATE CURRENT_TIMESTAMP)
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    // Negocios favoritos (solo aplica a usuarios CLIENT; se valida en la capa de servicio).
    // client_favorite_business.created_at lo llena la BD.
    @ManyToMany
    @JoinTable(name = "client_favorite_business",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "business_id"))
    private Set<Business> favoriteBusinesses = new HashSet<>();

    public User(Role role, String fullName, String email, String password) {
        this.role = role;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
    }
}
