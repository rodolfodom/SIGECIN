package com.sigecin.auth.exception;

import com.sigecin.common.exception.BusinessRuleException;

public class EmailAlreadyRegisteredException extends BusinessRuleException {

    public EmailAlreadyRegisteredException() {
        super("auth.register.email-taken");
    }
}
