package com.sigecin.business.exception;

import com.sigecin.common.exception.BusinessRuleException;

/** Cada usuario con rol BUSINESS tiene un solo negocio. */
public class BusinessAlreadyExistsException extends BusinessRuleException {

    public BusinessAlreadyExistsException() {
        super("business.setup.already-exists");
    }
}
