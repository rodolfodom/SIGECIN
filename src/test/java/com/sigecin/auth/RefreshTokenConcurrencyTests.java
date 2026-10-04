package com.sigecin.auth;

import com.sigecin.TestcontainersConfiguration;
import com.sigecin.user.User;
import com.sigecin.user.UserRepository;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dos renovaciones simultáneas con el mismo refresh token (p. ej. dos pestañas):
 * el bloqueo de fila las serializa, una rota y la otra entra por la ventana de gracia.
 * Sin transacción de prueba, para que cada renovación confirme la suya.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RefreshTokenConcurrencyTests {

    @Autowired RefreshTokenService refreshTokens;
    @Autowired UserRepository users;
    @Autowired JdbcClient jdbc;

    // Se repite porque el choque de bloqueos depende de dónde cae el hash del token nuevo en el índice
    @RepeatedTest(20)
    void simultaneousRenewalsDoNotEndTheSession() throws Exception {
        User user = users.findByEmail("laura.sanchez@unam.mx").orElseThrow();
        String raw = refreshTokens.create(user);
        String familyId = jdbc.sql("select family_id from refresh_token where token_hash = :hash")
                .param("hash", RefreshTokenService.hash(raw)).query(String.class).single();

        CountDownLatch start = new CountDownLatch(1);
        Callable<Optional<RefreshTokenService.Renewal>> renew = () -> {
            start.await();
            return refreshTokens.renew(raw);
        };
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Optional<RefreshTokenService.Renewal>>> futures = List.of(pool.submit(renew), pool.submit(renew));
            start.countDown();
            var first = futures.get(0).get();
            var second = futures.get(1).get();

            assertThat(first).isPresent();
            assertThat(second).isPresent();
            // Exactamente una de las dos rotó; la otra se aceptó sin emitir refresh nuevo
            assertThat(java.util.stream.Stream.of(first, second)
                    .filter(r -> r.get().newRefreshToken() != null)).hasSize(1);
            // La familia sigue viva: un token vigente (el rotado) y ninguno revocado sin reemplazo
            assertThat(jdbc.sql("select count(*) from refresh_token where family_id = :f and revoked_at is null")
                    .param("f", familyId).query(Long.class).single()).isEqualTo(1);
            assertThat(jdbc.sql("select count(*) from refresh_token where family_id = :f and revoked_at is not null and replaced_by_id is null")
                    .param("f", familyId).query(Long.class).single()).isZero();
        } finally {
            pool.shutdownNow();
            jdbc.sql("update refresh_token set replaced_by_id = null where family_id = :f").param("f", familyId).update();
            jdbc.sql("delete from refresh_token where family_id = :f").param("f", familyId).update();
        }
    }
}
