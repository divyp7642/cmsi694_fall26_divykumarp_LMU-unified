
package edu.lmu.unified.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class EventRepository {

    private static final String EVENT_KEY_PREFIX =
            "lmu:submitted-events:";

    private static final String EVENT_IDS_KEY =
            "lmu:submitted-events:ids";

    private static final String EVENT_SEQUENCE_KEY =
            "lmu:submitted-events:sequence";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public EventRepository(
            StringRedisTemplate redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    // Generate unique IDs without colliding with events 1, 2, and 3.
    public Long generateEventId() {
        Long sequence = redisTemplate.opsForValue()
                .increment(EVENT_SEQUENCE_KEY);

        if (sequence == null) {
            throw new IllegalStateException(
                    "Unable to generate event ID"
            );
        }

        return sequence + 3L;
    }

    // Save a submitted event in Redis.
    public void save(Event event) {
        try {
            String json = objectMapper.writeValueAsString(event);

            String eventKey = EVENT_KEY_PREFIX + event.id();

            redisTemplate.opsForValue().set(eventKey, json);

            redisTemplate.opsForSet().add(
                    EVENT_IDS_KEY,
                    event.id().toString()
            );

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Unable to serialize event",
                    exception
            );
        }
    }

    // Retrieve a submitted event by ID.
    public Optional<Event> findById(Long id) {
        String json = redisTemplate.opsForValue().get(
                EVENT_KEY_PREFIX + id
        );

        if (json == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(
                    objectMapper.readValue(json, Event.class)
            );

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Unable to deserialize event",
                    exception
            );
        }
    }

    // Retrieve all submitted events.
    public List<Event> findAll() {
        var eventIds = redisTemplate.opsForSet()
                .members(EVENT_IDS_KEY);

        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }

        return eventIds.stream()
                .map(Long::valueOf)
                .sorted()
                .map(this::findById)
                .flatMap(Optional::stream)
                .toList();
    }
}
