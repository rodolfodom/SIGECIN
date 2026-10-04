package com.sigecin.appointment.exception;

import com.sigecin.common.exception.BusinessRuleException;

/**
 * La cita ya no admite el cambio pedido: está cancelada, ya empezó, o (al confirmar)
 * ya no está pendiente. Puede ocurrir si otra acción simultánea la modificó.
 */
public class AppointmentNotModifiableException extends BusinessRuleException {

    public AppointmentNotModifiableException(String messageKey) {
        super(messageKey);
    }
}
