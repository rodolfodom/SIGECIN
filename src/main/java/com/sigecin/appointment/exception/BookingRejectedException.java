package com.sigecin.appointment.exception;

import com.sigecin.common.exception.BusinessRuleException;

/**
 * La reserva no cumple una regla de negocio. La clave del mensaje indica cuál:
 * servicio o negocio no disponible, fecha pasada, fuera del horario u horario ocupado.
 */
public class BookingRejectedException extends BusinessRuleException {

    public static final String UNAVAILABLE = "booking.error.unavailable";
    public static final String PAST = "booking.error.past";
    public static final String OUTSIDE_HOURS = "booking.error.outside-hours";
    public static final String TAKEN = "booking.error.taken";

    public BookingRejectedException(String messageKey) {
        super(messageKey);
    }
}
