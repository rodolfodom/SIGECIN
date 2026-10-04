package com.sigecin.config;

import com.sigecin.business.service.BusinessService;
import com.sigecin.business.web.BusinessSetupInterceptor;
import com.sigecin.common.web.CookieFlashMapManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final BusinessService businessService;

    public WebConfig(BusinessService businessService) {
        this.businessService = businessService;
    }

    // Un dueño sin negocio registrado es llevado a /business/setup
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new BusinessSetupInterceptor(businessService)).addPathPatterns("/business/**");
    }

    // El DispatcherServlet busca este bean por nombre; reemplaza al que usa la sesión
    @Bean(name = DispatcherServlet.FLASH_MAP_MANAGER_BEAN_NAME)
    FlashMapManager flashMapManager(AuthProperties properties) {
        return new CookieFlashMapManager(properties.cookieSecure());
    }
}
