package com.sigecin.auth.security;

import com.sigecin.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Token CSRF en cookie contra un servidor real: con MockMvc, spring-security-test reemplaza el
 * repositorio de tokens CSRF por uno en sesión y no se puede observar la cookie.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CsrfCookieTests {

    private static final Pattern CSRF_FIELD = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");

    @LocalServerPort int port;

    private final HttpClient http = HttpClient.newBuilder()
            .cookieHandler(new CookieManager())
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Test
    void logoutWorksFromAPageLoadedEarlier() throws Exception {
        // El token no debe rotar en cada página: el formulario de otra pestaña o del historial sigue siendo válido
        HttpResponse<String> login = post("/login", "email=carlos.ramirez@unam.mx&password=pass1234&_csrf="
                + csrfToken(get("/login")));
        assertThat(login.headers().firstValue("Location").orElseThrow()).endsWith("/businesses");

        String earlierPageToken = csrfToken(get("/businesses"));
        get("/favorites");

        HttpResponse<String> logout = post("/logout", "_csrf=" + earlierPageToken);
        assertThat(logout.statusCode()).isEqualTo(302);
        assertThat(logout.headers().firstValue("Location").orElseThrow()).endsWith("/login?logout");
    }

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String form) throws Exception {
        return http.send(HttpRequest.newBuilder(uri(path))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private static String csrfToken(HttpResponse<String> page) {
        Matcher matcher = CSRF_FIELD.matcher(page.body());
        assertThat(matcher.find()).as("campo _csrf en la página").isTrue();
        return URLEncoder.encode(matcher.group(1), StandardCharsets.UTF_8);
    }
}
