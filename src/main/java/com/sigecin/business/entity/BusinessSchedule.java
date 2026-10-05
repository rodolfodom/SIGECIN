package com.sigecin.business.entity;

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

import java.time.DayOfWeek;
import java.time.LocalTime;

/** Horario de atención de un día; máximo uno por día de la semana y negocio. */
@Entity
@Table(name = "business_schedule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BusinessSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "open_time", nullable = false)
    @Setter
    private LocalTime openTime;

    @Column(name = "close_time", nullable = false)
    @Setter
    private LocalTime closeTime;

    // Permite deshabilitar un día sin borrarlo
    @Column(nullable = false)
    @Setter
    private boolean active = true;

    public BusinessSchedule(Business business, DayOfWeek dayOfWeek, LocalTime openTime, LocalTime closeTime) {
        this.business = business;
        this.dayOfWeek = dayOfWeek;
        this.openTime = openTime;
        this.closeTime = closeTime;
    }
}
