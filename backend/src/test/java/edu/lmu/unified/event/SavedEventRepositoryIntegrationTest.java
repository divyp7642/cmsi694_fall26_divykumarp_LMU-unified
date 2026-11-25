package edu.lmu.unified.event;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SavedEventRepositoryIntegrationTest {

    private static LettuceConnectionFactory connectionFactory;

    private static StringRedisTemplate redisTemplate;

    private SavedEventRepository repository;

    private static final String STUDENT_ONE =
            "saved-event-test-one@lion.lmu.edu";

    private static final String STUDENT_TWO =
            "saved-event-test-two@lion.lmu.edu";

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

    private static void cleanTestData() {

        if (redisTemplate != null) {

            redisTemplate.delete(List.of(
                    "lmu:students:" + STUDENT_ONE
                            + ":saved-events",

                    "lmu:students:" + STUDENT_TWO
                            + ":saved-events"
            ));
        }
    }

    @BeforeEach
    void setUp() {

        cleanTestData();

        repository =
                new SavedEventRepository(redisTemplate);
    }

    @AfterAll
    static void disconnectFromRedis() {

        try {
            cleanTestData();
        } finally {
            if (connectionFactory != null) {
                connectionFactory.destroy();
            }
        }
    }

    // Test 1 - Save and retrieve an event.
    @Test
    void shouldSaveAndRetrieveEvent() {

        assertTrue(
                repository.saveEvent(1L, STUDENT_ONE)
        );

        assertEquals(
                List.of(1L),
                repository.getSavedEventIds(STUDENT_ONE)
        );
    }

    // Test 2 - Prevent duplicate saved events.
    @Test
    void shouldPreventDuplicateSavedEvents() {

        assertTrue(
                repository.saveEvent(1L, STUDENT_ONE)
        );

        assertFalse(
                repository.saveEvent(1L, STUDENT_ONE)
        );

        assertEquals(
                List.of(1L),
                repository.getSavedEventIds(STUDENT_ONE)
        );
    }

    // Test 3 - Save multiple events.
    @Test
    void shouldSaveMultipleEvents() {

        assertTrue(
                repository.saveEvent(3L, STUDENT_ONE)
        );

        assertTrue(
                repository.saveEvent(1L, STUDENT_ONE)
        );

        assertTrue(
                repository.saveEvent(2L, STUDENT_ONE)
        );

        assertEquals(
                List.of(1L, 2L, 3L),
                repository.getSavedEventIds(STUDENT_ONE)
        );
    }

    // Test 4 - Remove a saved event.
    @Test
    void shouldRemoveSavedEvent() {

        repository.saveEvent(1L, STUDENT_ONE);
        repository.saveEvent(2L, STUDENT_ONE);

        assertTrue(
                repository.removeSavedEvent(
                        1L,
                        STUDENT_ONE
                )
        );

        assertFalse(
                repository.isEventSaved(1L, STUDENT_ONE)
        );

        assertEquals(
                List.of(2L),
                repository.getSavedEventIds(STUDENT_ONE)
        );
    }

    // Test 5 - Handle repeated removal safely.
    @Test
    void shouldHandleRepeatedRemoval() {

        repository.saveEvent(1L, STUDENT_ONE);

        assertTrue(
                repository.removeSavedEvent(
                        1L,
                        STUDENT_ONE
                )
        );

        assertFalse(
                repository.removeSavedEvent(
                        1L,
                        STUDENT_ONE
                )
        );

        assertTrue(
                repository.getSavedEventIds(STUDENT_ONE)
                        .isEmpty()
        );
    }

    // Test 6 - Check whether an event is saved.
    @Test
    void shouldCheckSavedEventStatus() {

        assertFalse(
                repository.isEventSaved(1L, STUDENT_ONE)
        );

        repository.saveEvent(1L, STUDENT_ONE);

        assertTrue(
                repository.isEventSaved(1L, STUDENT_ONE)
        );

        assertFalse(
                repository.isEventSaved(2L, STUDENT_ONE)
        );
    }

    // Test 7 - Verify student account isolation.
    @Test
    void shouldIsolateSavedEventsBetweenStudents() {

        repository.saveEvent(1L, STUDENT_ONE);
        repository.saveEvent(2L, STUDENT_TWO);

        assertFalse(
                repository.isEventSaved(1L, STUDENT_TWO)
        );

        assertFalse(
                repository.removeSavedEvent(
                        1L,
                        STUDENT_TWO
                )
        );

        assertEquals(
                List.of(1L),
                repository.getSavedEventIds(STUDENT_ONE)
        );

        assertEquals(
                List.of(2L),
                repository.getSavedEventIds(STUDENT_TWO)
        );

        // Both students can independently save the same event.
        assertTrue(
                repository.saveEvent(1L, STUDENT_TWO)
        );

        assertTrue(
                repository.removeSavedEvent(
                        1L,
                        STUDENT_TWO
                )
        );

        assertTrue(
                repository.isEventSaved(1L, STUDENT_ONE)
        );
    }

    // Test 8 - Verify email normalization.
    @Test
    void shouldNormalizeStudentEmail() {

        String formattedEmail =
                "  " + STUDENT_ONE.toUpperCase(Locale.ROOT)
                        + "  ";

        assertTrue(
                repository.saveEvent(1L, formattedEmail)
        );

        assertTrue(
                repository.isEventSaved(1L, STUDENT_ONE)
        );

        assertFalse(
                repository.saveEvent(1L, STUDENT_ONE)
        );

        assertEquals(
                List.of(1L),
                repository.getSavedEventIds(formattedEmail)
        );
    }

    // Test 9 - Verify persistence across repository instances.
    @Test
    void shouldPersistAcrossRepositoryInstances() {

        repository.saveEvent(1L, STUDENT_ONE);
        repository.saveEvent(3L, STUDENT_ONE);

        SavedEventRepository newRepository =
                new SavedEventRepository(redisTemplate);

        assertEquals(
                List.of(1L, 3L),
                newRepository.getSavedEventIds(STUDENT_ONE)
        );

        assertTrue(
                newRepository.isEventSaved(
                        1L,
                        STUDENT_ONE
                )
        );
    }

    // Test 10 - Prevent concurrent duplicate saves.
    @Test
    void shouldPreventConcurrentDuplicateSaves()
            throws Exception {

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

                        return repository.saveEvent(
                                1L,
                                STUDENT_ONE
                        );
                    });

            Future<Boolean> secondResult =
                    executor.submit(() -> {

                        ready.countDown();
                        start.await();

                        return repository.saveEvent(
                                1L,
                                STUDENT_ONE
                        );
                    });

            assertTrue(
                    ready.await(5, TimeUnit.SECONDS)
            );

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

            // Exactly one save operation must add the event.
            assertNotEquals(
                    firstSaved,
                    secondSaved
            );

            assertEquals(
                    List.of(1L),
                    repository.getSavedEventIds(STUDENT_ONE)
            );

        } finally {

            start.countDown();
            executor.shutdownNow();
        }
    }

    // Test 11 - Retrieve an empty saved-event list.
    @Test
    void shouldReturnEmptySavedEventList() {

        assertNotNull(
                repository.getSavedEventIds(STUDENT_ONE)
        );

        assertTrue(
                repository.getSavedEventIds(STUDENT_ONE)
                        .isEmpty()
        );
    }
}