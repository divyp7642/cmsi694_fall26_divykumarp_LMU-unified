package edu.lmu.unified.room;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class RoomReservationRepositoryIntegrationTest {

    private static LettuceConnectionFactory connectionFactory;

    private static StringRedisTemplate redisTemplate;

    private RoomReservationRepository repository;

    private final LocalDate reservationDate =
            LocalDate.of(2026, 11, 20);

    @BeforeAll
    static void connectToRedis() {

        RedisStandaloneConfiguration configuration =
                new RedisStandaloneConfiguration(
                        "127.0.0.1",
                        6380
                );

        // Dedicated integration test database.
        configuration.setDatabase(15);

        connectionFactory =
                new LettuceConnectionFactory(configuration);

        connectionFactory.afterPropertiesSet();
        connectionFactory.start();

        redisTemplate =
                new StringRedisTemplate(connectionFactory);

        redisTemplate.afterPropertiesSet();
    }

    @BeforeEach
    void setUp() {

        // Delete only room reservation test keys.
        Set<String> keys = redisTemplate.keys(
                "lmu:room-reservations:*"
        );

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        repository =
                new RoomReservationRepository(redisTemplate);
    }

    @AfterAll
    static void disconnectFromRedis() {

        if (redisTemplate != null) {

            Set<String> keys = redisTemplate.keys(
                    "lmu:room-reservations:*"
            );

            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }

        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    private RoomReservation createReservation(
            Long roomId,
            Long eventId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            ReservationStatus status
    ) {

        return new RoomReservation(
                repository.generateReservationId(),
                roomId,
                eventId,
                "organizer@lion.lmu.edu",
                date,
                startTime,
                endTime,
                status
        );
    }

    private RoomReservation createConfirmedReservation() {

        return createReservation(
                101L,
                5L,
                reservationDate,
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                ReservationStatus.CONFIRMED
        );
    }

    @Test
    void shouldSaveAndRetrieveConfirmedReservation() {

        RoomReservation reservation =
                createConfirmedReservation();

        assertTrue(
                repository.saveIfAvailable(reservation)
        );

        RoomReservation retrieved =
                repository.findById(reservation.id())
                        .orElseThrow();

        assertEquals(reservation, retrieved);

        assertEquals(
                1,
                repository.findAll().size()
        );
    }

    @Test
    void shouldRejectOverlappingReservation() {

        RoomReservation first =
                createConfirmedReservation();

        assertTrue(repository.saveIfAvailable(first));

        RoomReservation second =
                createReservation(
                        101L,
                        6L,
                        reservationDate,
                        LocalTime.of(11, 0),
                        LocalTime.of(13, 0),
                        ReservationStatus.CONFIRMED
                );

        assertFalse(
                repository.saveIfAvailable(second)
        );

        assertTrue(
                repository.findById(second.id()).isEmpty()
        );

        assertEquals(
                1,
                repository.findAll().size()
        );
    }

    @Test
    void shouldAllowAdjacentReservation() {

        RoomReservation first =
                createConfirmedReservation();

        assertTrue(repository.saveIfAvailable(first));

        RoomReservation second =
                createReservation(
                        101L,
                        6L,
                        reservationDate,
                        LocalTime.of(12, 0),
                        LocalTime.of(14, 0),
                        ReservationStatus.CONFIRMED
                );

        assertTrue(
                repository.saveIfAvailable(second)
        );

        assertEquals(
                2,
                repository.findAll().size()
        );
    }

    @Test
    void shouldAllowReservationInDifferentRoom() {

        RoomReservation first =
                createConfirmedReservation();

        assertTrue(repository.saveIfAvailable(first));

        RoomReservation second =
                createReservation(
                        102L,
                        6L,
                        reservationDate,
                        LocalTime.of(10, 0),
                        LocalTime.of(12, 0),
                        ReservationStatus.CONFIRMED
                );

        assertTrue(
                repository.saveIfAvailable(second)
        );

        assertEquals(
                2,
                repository.findAll().size()
        );
    }

    @Test
    void shouldAllowReservationOnDifferentDate() {

        RoomReservation first =
                createConfirmedReservation();

        assertTrue(repository.saveIfAvailable(first));

        RoomReservation second =
                createReservation(
                        101L,
                        6L,
                        reservationDate.plusDays(1),
                        LocalTime.of(10, 0),
                        LocalTime.of(12, 0),
                        ReservationStatus.CONFIRMED
                );

        assertTrue(
                repository.saveIfAvailable(second)
        );

        assertEquals(
                2,
                repository.findAll().size()
        );
    }

    @Test
    void shouldIgnoreCancelledReservations() {

        RoomReservation cancelled =
                createReservation(
                        101L,
                        5L,
                        reservationDate,
                        LocalTime.of(10, 0),
                        LocalTime.of(12, 0),
                        ReservationStatus.CANCELLED
                );

        repository.save(cancelled);

        RoomReservation confirmed =
                createReservation(
                        101L,
                        6L,
                        reservationDate,
                        LocalTime.of(11, 0),
                        LocalTime.of(13, 0),
                        ReservationStatus.CONFIRMED
                );

        assertTrue(
                repository.saveIfAvailable(confirmed)
        );

        List<RoomReservation> confirmedReservations =
                repository.findConfirmedByRoomIdAndDate(
                        101L,
                        reservationDate
                );

        assertEquals(
                List.of(confirmed),
                confirmedReservations
        );
    }

    @Test
    void shouldIgnorePendingReservations() {

        RoomReservation pending =
                createReservation(
                        101L,
                        5L,
                        reservationDate,
                        LocalTime.of(10, 0),
                        LocalTime.of(12, 0),
                        ReservationStatus.PENDING
                );

        repository.save(pending);

        RoomReservation confirmed =
                createReservation(
                        101L,
                        6L,
                        reservationDate,
                        LocalTime.of(11, 0),
                        LocalTime.of(13, 0),
                        ReservationStatus.CONFIRMED
                );

        assertTrue(
                repository.saveIfAvailable(confirmed)
        );

        List<RoomReservation> confirmedReservations =
                repository.findConfirmedByRoomIdAndDate(
                        101L,
                        reservationDate
                );

        assertEquals(
                List.of(confirmed),
                confirmedReservations
        );
    }

    @Test
    void shouldPreventConcurrentDoubleBooking()
            throws Exception {

        RoomReservation first =
                createConfirmedReservation();

        RoomReservation second =
                createReservation(
                        101L,
                        6L,
                        reservationDate,
                        LocalTime.of(11, 0),
                        LocalTime.of(13, 0),
                        ReservationStatus.CONFIRMED
                );

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch ready =
                new CountDownLatch(2);

        CountDownLatch start =
                new CountDownLatch(1);

        try {

            Future<Boolean> firstResult =
                    executor.submit(() -> {

                        ready.countDown();
                        start.await();

                        return repository.saveIfAvailable(
                                first
                        );
                    });

            Future<Boolean> secondResult =
                    executor.submit(() -> {

                        ready.countDown();
                        start.await();

                        return repository.saveIfAvailable(
                                second
                        );
                    });

            assertTrue(
                    ready.await(5, TimeUnit.SECONDS)
            );

            // Release both requests together.
            start.countDown();

            boolean firstSaved =
                    firstResult.get(
                            10,
                            TimeUnit.SECONDS
                    );

            boolean secondSaved =
                    secondResult.get(
                            10,
                            TimeUnit.SECONDS
                    );

            // Exactly one reservation must succeed.
            assertNotEquals(
                    firstSaved,
                    secondSaved
            );

            assertEquals(
                    1,
                    repository.findAll().size()
            );

            assertEquals(
                    1,
                    repository.findConfirmedByRoomIdAndDate(
                            101L,
                            reservationDate
                    ).size()
            );

        } finally {

            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void shouldRejectDirectSavingOfConfirmedReservation() {

        RoomReservation reservation =
                createConfirmedReservation();

        assertThrows(
                IllegalArgumentException.class,
                () -> repository.save(reservation)
        );

        assertTrue(
                repository.findAll().isEmpty()
        );
    }
}