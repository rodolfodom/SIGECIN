package com.sigecin.common.exception;

/**
 * Violación de una regla de negocio. Lleva la clave del mensaje (messages.properties)
 * que se muestra al usuario, normalmente como mensaje flash.
 */
public class BusinessRuleException extends RuntimeException {

    private final String messageKey;

    public BusinessRuleException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
