package com.sigecin.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * El recurso no existe o pertenece a otro usuario. En ambos casos se responde 404
 * (no 403) para no revelar su existencia; se muestra templates/error/404.html.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
