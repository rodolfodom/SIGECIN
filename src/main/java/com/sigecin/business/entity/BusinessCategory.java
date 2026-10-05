package com.sigecin.business.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Catálogo de categorías; el nombre está en español porque se muestra al usuario. */
@Entity
@Table(name = "business_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BusinessCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer id;

    @Column(nullable = false, length = 80, unique = true)
    private String name;
}
