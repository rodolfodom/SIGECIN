package com.sigecin.config;

import com.sigecin.common.web.CookieFlashMapManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMapManager;

@Configuration
public class WebConfig {

    // El DispatcherServlet busca este bean por nombre; reemplaza al que usa la sesión
    @Bean(name = DispatcherServlet.FLASH_MAP_MANAGER_BEAN_NAME)
    FlashMapManager flashMapManager(AuthProperties properties) {
        return new CookieFlashMapManager(properties.cookieSecure());
    }
}
