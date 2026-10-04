package com.sigecin.common.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.AbstractFlashMapManager;
import org.springframework.web.util.WebUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Guarda los atributos flash (patrón Post/Redirect/Get) en una cookie en lugar
 * de la sesión HTTP, porque la aplicación no usa sesión del lado del servidor.
 * Solo admite valores de texto: los mensajes flash son cadenas.
 */
public class CookieFlashMapManager extends AbstractFlashMapManager {

    static final String COOKIE_NAME = "SIGECIN_FLASH";

    private final ObjectMapper mapper = new ObjectMapper();
    private final boolean secure;

    public CookieFlashMapManager(boolean secure) {
        this.secure = secure;
    }

    @Override
    protected List<FlashMap> retrieveFlashMaps(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, COOKIE_NAME);
        if (cookie == null || cookie.getValue().isBlank()) {
            return null;
        }
        try {
            byte[] json = Base64.getUrlDecoder().decode(cookie.getValue());
            List<StoredFlashMap> stored = mapper.readValue(json, new TypeReference<>() { });
            return new ArrayList<>(stored.stream().map(StoredFlashMap::toFlashMap).toList());
        } catch (Exception e) {
            // Cookie alterada o de una versión anterior: se descarta
            return null;
        }
    }

    @Override
    protected void updateFlashMaps(List<FlashMap> flashMaps, HttpServletRequest request,
                                   HttpServletResponse response) {
        String value = "";
        if (!flashMaps.isEmpty()) {
            try {
                List<StoredFlashMap> stored = flashMaps.stream().map(StoredFlashMap::from).toList();
                value = Base64.getUrlEncoder().withoutPadding().encodeToString(mapper.writeValueAsBytes(stored));
            } catch (Exception e) {
                throw new IllegalStateException("No se pudieron guardar los atributos flash", e);
            }
        }
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(value.isEmpty() ? Duration.ZERO : Duration.ofSeconds(getFlashMapTimeout()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /** Forma serializable de un FlashMap. */
    record StoredFlashMap(Map<String, String> attributes, String targetPath,
                          Map<String, List<String>> targetParams, long expirationTime) {

        static StoredFlashMap from(FlashMap flashMap) {
            Map<String, String> attributes = new LinkedHashMap<>();
            flashMap.forEach((key, value) -> {
                if (!(value instanceof String text)) {
                    throw new IllegalArgumentException("El atributo flash '" + key + "' debe ser texto");
                }
                attributes.put(key, text);
            });
            return new StoredFlashMap(attributes, flashMap.getTargetRequestPath(),
                    flashMap.getTargetRequestParams(), flashMap.getExpirationTime());
        }

        FlashMap toFlashMap() {
            FlashMap flashMap = new FlashMap();
            flashMap.putAll(attributes);
            flashMap.setTargetRequestPath(targetPath);
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            if (targetParams != null) {
                params.putAll(targetParams);
            }
            flashMap.addTargetRequestParams(params);
            flashMap.setExpirationTime(expirationTime);
            return flashMap;
        }
    }
}
