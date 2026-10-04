package com.sigecin.appointment.service;

import com.sigecin.IntegrationTest;
import com.sigecin.appointment.exception.BookingRejectedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reservas simultáneas reales: cada hilo usa su propia transacción (sin transacción de
 * prueba), igual que dos clientes que reservan al mismo tiempo. Debe ganar exactamente
 * una y las demás deben rechazarse por horario ocupado, sin deadlocks ni otros errores.
 */
@IntegrationTest
class BookingConcurrencyTests {

    private static final int THREADS = 8;
    // Clientes activos del script (Sofía, el 4, está inactiva pero puede reservar igual: no importa aquí)
    private static final long[] CLIENTS = {1L, 2L, 3L};

    @Autowired BookingService booking;
    @Autowired JdbcClient jdbc;

    private final List<Long> created = new ArrayList<>();

    @AfterEach
    void deleteCreatedAppointments() {
        if (!created.isEmpty()) {
            jdbc.sql("delete from appointment where id in (:ids)").param("ids", created).update();
        }
    }

    // Se repite: el choque depende del orden en que los hilos llegan al bloqueo
    @RepeatedTest(10)
    void sameSlotIsBookedOnlyOnce() throws Exception {
        LocalDateTime slot = mondayInTwoMonths().atTime(10, 0);
        // Servicio 1 = "Corte clásico" (30 min) de la Barbería
        List<Outcome> outcomes = bookConcurrently(i -> slot, 1L);

        assertThat(outcomes).filteredOn(Outcome::booked).hasSize(1);
        assertThat(outcomes).filteredOn(o -> !o.booked())
                .allSatisfy(o -> assertThat(o.rejection()).isEqualTo(BookingRejectedException.TAKEN));
    }

    @RepeatedTest(5)
    void overlappingSlotsOfDifferentServicesAreBookedOnlyOnce() throws Exception {
        LocalDateTime base = mondayInTwoMonths().atTime(12, 0);
        // Alterna "Corte clásico" (30 min) a las 12:00 y "Corte + barba" (50 min) a las 12:15: todas se traslapan
        List<Outcome> outcomes = bookConcurrently(i -> i % 2 == 0 ? base : base.plusMinutes(15), null);

        assertThat(outcomes).filteredOn(Outcome::booked).hasSize(1);
    }

    private record Outcome(boolean booked, String rejection) {
    }

    private interface StartFor {
        LocalDateTime at(int thread);
    }

    /** Lanza THREADS reservas a la vez; {@code serviceId} null alterna entre los servicios 1 y 2. */
    private List<Outcome> bookConcurrently(StartFor startFor, Long serviceId) throws Exception {
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            List<Future<Outcome>> futures = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                int thread = i;
                Callable<Outcome> task = () -> {
                    go.await();
                    long service = serviceId != null ? serviceId : (thread % 2 == 0 ? 1L : 2L);
                    try {
                        Long id = booking.book(CLIENTS[thread % CLIENTS.length], service, startFor.at(thread), null).getId();
                        synchronized (created) {
                            created.add(id);
                        }
                        return new Outcome(true, null);
                    } catch (BookingRejectedException e) {
                        return new Outcome(false, e.getMessageKey());
                    }
                };
                futures.add(pool.submit(task));
            }
            go.countDown();
            List<Outcome> outcomes = new ArrayList<>();
            for (Future<Outcome> future : futures) {
                try {
                    outcomes.add(future.get());
                } catch (ExecutionException e) {
                    // Deadlock, timeout de bloqueo o cualquier otro error: la prueba debe fallar
                    throw new AssertionError("Una reserva falló con un error inesperado", e.getCause());
                }
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }

    private static LocalDate mondayInTwoMonths() {
        // Lejos de los datos de prueba del script (mes en curso) y en un día que la Barbería abre
        return LocalDate.now().plusMonths(2).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
    }
}
