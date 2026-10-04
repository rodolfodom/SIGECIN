package com.sigecin.business.web;

import com.sigecin.auth.security.AuthenticatedUser;
import com.sigecin.business.service.BusinessService;
import com.sigecin.user.enums.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * En /business/**: un dueño sin negocio registrado solo puede usar /business/setup,
 * y uno que ya lo tiene no puede volver a darlo de alta.
 */
public class BusinessSetupInterceptor implements HandlerInterceptor {

    static final String SETUP_PATH = "/business/setup";

    private final BusinessService businessService;

    public BusinessSetupInterceptor(BusinessService businessService) {
        this.businessService = businessService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user) || user.role() != Role.BUSINESS) {
            return true;
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean onSetup = path.equals(SETUP_PATH);
        boolean hasBusiness = businessService.hasBusiness(user.id());
        if (!hasBusiness && !onSetup) {
            response.sendRedirect(request.getContextPath() + SETUP_PATH);
            return false;
        }
        if (hasBusiness && onSetup) {
            response.sendRedirect(request.getContextPath() + "/business/profile");
            return false;
        }
        return true;
    }
}
