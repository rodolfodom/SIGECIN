package com.sigecin.api.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigecin.IntegrationTest;
import com.sigecin.support.AppointmentTestData;
import com.sigecin.support.AuthTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** API REST con JWT en el encabezado Authorization y refresh token en el cuerpo (flujo de Postman). */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class ApiTests {

    // Datos de prueba de ddl_sigecin.sql
    private static final String CLIENT_EMAIL = "carlos.ramirez@unam.mx";
    private static final String CLIENT_PASSWORD = "pass1234";
    private static final String INACTIVE_EMAIL = "sofia.herrera@unam.mx";
    private static final long BARBERIA = 1L;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AuthTestSupport auth;
    @Autowired EntityManager em;

    // --- Autenticación ---

    @Test
    void loginReturnsAccessAndRefreshTokens() throws Exception {
        JsonNode tokens = login(CLIENT_EMAIL, CLIENT_PASSWORD);

        assertThat(tokens.get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(tokens.get("accessToken").asText()).isNotBlank();
        assertThat(tokens.get("expiresIn").asLong()).isEqualTo(15 * 60);
        assertThat(tokens.get("refreshToken").asText()).isNotBlank();
        assertThat(tokens.get("refreshExpiresIn").asLong()).isEqualTo(7 * 24 * 3600);

        mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.get("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(CLIENT_EMAIL))
                .andExpect(jsonPath("$.role").value("CLIENT"));
    }

    @Test
    void loginRejectsBadCredentialsAndInactiveAccounts() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", CLIENT_EMAIL, "password", "incorrecta"))))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", INACTIVE_EMAIL, "password", CLIENT_PASSWORD))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpointsRequireTheBearerHeader() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized())
                .andExpect(header().exists(HttpHeaders.WWW_AUTHENTICATE));
        mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
        // La cookie de las vistas no sirve en la API
        mvc.perform(get("/api/me").cookie(auth.cookieFor(CLIENT_EMAIL)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRotatesTheTokenAndLogoutRevokesIt() throws Exception {
        String first = login(CLIENT_EMAIL, CLIENT_PASSWORD).get("refreshToken").asText();

        JsonNode renewed = refresh(first, 200);
        String second = renewed.get("refreshToken").asText();
        assertThat(second).isNotBlank().isNotEqualTo(first);
        assertThat(renewed.get("accessToken").asText()).isNotBlank();

        // Token recién rotado dentro de la ventana de gracia: nuevo JWT, sin otro refresh token
        assertThat(refresh(first, 200).has("refreshToken")).isFalse();

        mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("refreshToken", second))))
                .andExpect(status().isNoContent());
        // La revocación es un UPDATE masivo: en la transacción compartida de la prueba hay que
        // descartar las entidades en caché (en la aplicación cada petición tiene su transacción)
        em.clear();
        refresh(second, 401);
        refresh("no-existe", 401);
    }

    // --- Roles ---

    @Test
    void rolesAreEnforced() throws Exception {
        mvc.perform(get("/api/business/appointments").header(HttpHeaders.AUTHORIZATION, auth.bearerFor(CLIENT_EMAIL)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/client/appointments")
                        .header(HttpHeaders.AUTHORIZATION, auth.bearerFor(AuthTestSupport.BARBERIA_OWNER)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/client/appointments").header(HttpHeaders.AUTHORIZATION, auth.bearerFor(CLIENT_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(not(0)));
    }

    // --- Catálogo público ---

    @Test
    void directoryIsPublic() throws Exception {
        mvc.perform(get("/api/businesses").param("name", "barber"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(BARBERIA))
                .andExpect(jsonPath("$.content[0].category").value(not(emptyString())));
        mvc.perform(get("/api/businesses/{id}", BARBERIA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.week.length()").value(7))
                .andExpect(jsonPath("$.services[*].id").value(hasItem((int) AppointmentTestData.CORTE_CLASICO)));
        mvc.perform(get("/api/businesses/{id}/availability", BARBERIA)
                        .param("serviceId", "" + AppointmentTestData.CORTE_CLASICO)
                        .param("date", AppointmentTestData.futureMonday().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(true))
                .andExpect(jsonPath("$.slots[0]").value("09:00:00"));
        mvc.perform(get("/api/businesses/{id}", 999_999)).andExpect(status().isNotFound());
    }

    // --- Citas ---

    @Test
    void clientBooksAndCancelsAndTheOwnerSeesIt() throws Exception {
        String client = auth.bearerFor(CLIENT_EMAIL);
        String owner = auth.bearerFor(AuthTestSupport.BARBERIA_OWNER);
        LocalDateTime start = AppointmentTestData.futureMonday().atTime(10, 0);
        String booking = body(Map.of("serviceId", AppointmentTestData.CORTE_CLASICO,
                "startTime", start.toString(), "clientNotes", "Desde Postman"));

        String created = mvc.perform(post("/api/client/appointments").header(HttpHeaders.AUTHORIZATION, client)
                        .contentType(MediaType.APPLICATION_JSON).content(booking))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.businessId").value(BARBERIA))
                .andExpect(jsonPath("$.clientNotes").value("Desde Postman"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("id").asLong();

        // El mismo horario ya está ocupado: 422 con el texto de messages.properties
        mvc.perform(post("/api/client/appointments").header(HttpHeaders.AUTHORIZATION, client)
                        .contentType(MediaType.APPLICATION_JSON).content(booking))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("booking.error.taken"))
                .andExpect(jsonPath("$.detail").value(not(emptyString())));

        mvc.perform(get("/api/business/appointments").header(HttpHeaders.AUTHORIZATION, owner)
                        .param("from", start.toLocalDate().toString())
                        .param("to", start.toLocalDate().plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].appointmentId").value(hasItem((int) id)));

        String reasons = mvc.perform(get("/api/client/cancellation-reasons").header(HttpHeaders.AUTHORIZATION, client))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int reasonId = json.readTree(reasons).get(0).get("id").asInt();

        mvc.perform(post("/api/client/appointments/{id}/cancel", id).header(HttpHeaders.AUTHORIZATION, client)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("reasonId", reasonId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value(not(emptyString())));

        // Otro cliente no la ve
        mvc.perform(get("/api/client/appointments/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, auth.bearerFor("laura.sanchez@unam.mx")))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerAppointmentsRangeIsValidated() throws Exception {
        mvc.perform(get("/api/business/appointments")
                        .header(HttpHeaders.AUTHORIZATION, auth.bearerFor(AuthTestSupport.BARBERIA_OWNER))
                        .param("from", "2026-10-10").param("to", "2026-10-01"))
                .andExpect(status().isBadRequest());
    }

    private JsonNode login(String email, String password) throws Exception {
        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response);
    }

    private JsonNode refresh(String refreshToken, int expectedStatus) throws Exception {
        String response = mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("refreshToken", refreshToken))))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return response.isEmpty() ? null : json.readTree(response);
    }

    private String body(Map<String, ?> values) throws Exception {
        return json.writeValueAsString(values);
    }
}
