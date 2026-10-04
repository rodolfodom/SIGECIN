package com.sigecin.common.web;

/** Valida destinos de redirección recibidos en parámetros, para evitar redirecciones abiertas. */
public final class LocalRedirects {

    private LocalRedirects() {
    }

    /** Solo rutas de esta aplicación: empiezan con "/" y no son "//host" ni contienen "\\". */
    public static boolean isLocal(String target) {
        return target != null && target.startsWith("/") && !target.startsWith("//") && !target.contains("\\");
    }

    public static String orDefault(String target, String fallback) {
        return isLocal(target) ? target : fallback;
    }
}
