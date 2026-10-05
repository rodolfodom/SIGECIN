package com.sigecin.api.web;

import com.sigecin.common.exception.BusinessRuleException;
import com.sigecin.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Errores de la API como JSON (RFC 9457, application/problem+json). Las validaciones de
 * {@code @Valid} y los parámetros mal formados los resuelve la clase base con 400.
 * Las reglas de negocio usan el mismo texto que las vistas (messages.properties).
 */
@Slf4j
@RestControllerAdvice(basePackageClasses = ApiExceptionHandler.class)
@RequiredArgsConstructor
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private final MessageSource messages;

    @ExceptionHandler(DisabledException.class)
    ProblemDetail disabled(DisabledException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail unauthorized(AuthenticationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    ProblemDetail businessRule(BusinessRuleException e) {
        String text = messages.getMessage(e.getMessageKey(), null, e.getMessageKey(), LocaleContextHolder.getLocale());
        log.info("Petición de API rechazada por regla de negocio: {}", e.getMessageKey());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, text);
        problem.setProperty("code", e.getMessageKey());
        return problem;
    }
}
