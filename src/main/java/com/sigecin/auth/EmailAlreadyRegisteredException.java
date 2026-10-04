package com.sigecin.auth;

import com.sigecin.common.BusinessRuleException;

public class EmailAlreadyRegisteredException extends BusinessRuleException {

    public EmailAlreadyRegisteredException() {
        super("auth.register.email-taken");
    }
}
