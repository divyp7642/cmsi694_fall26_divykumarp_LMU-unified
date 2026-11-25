package edu.lmu.unified.event;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Repository
public class SavedEventRepository {

    private final StringRedisTemplate redisTemplate;

    public SavedEventRepository(
            StringRedisTemplate redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    private String getStudentKey(String studentEmail) {
        return "lmu:students:"
                + studentEmail.trim().toLowerCase(Locale.ROOT)
                + ":saved-events";
    }

    // SCRUM-28 - Save an event and prevent duplicates.
    public boolean saveEvent(
            Long eventId,
            String studentEmail
    ) {
        Long added = redisTemplate.opsForSet().add(
                getStudentKey(studentEmail),
                eventId.toString()
        );

        return added != null && added > 0;
    }

    // SCRUM-28 - Remove a saved event.
    public boolean removeSavedEvent(
            Long eventId,
            String studentEmail
    ) {
        Long removed = redisTemplate.opsForSet().remove(
                getStudentKey(studentEmail),
                eventId.toString()
        );

        return removed != null && removed > 0;
    }

    // SCRUM-28 - Check whether an event is already saved.
    public boolean isEventSaved(
            Long eventId,
            String studentEmail
    ) {
        Boolean exists = redisTemplate.opsForSet().isMember(
                getStudentKey(studentEmail),
                eventId.toString()
        );

        return Boolean.TRUE.equals(exists);
    }

    // SCRUM-28 - Retrieve saved event IDs for a student.
    public List<Long> getSavedEventIds(
            String studentEmail
    ) {
        Set<String> eventIds = redisTemplate.opsForSet()
                .members(getStudentKey(studentEmail));

        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }

        return eventIds.stream()
                .map(Long::valueOf)
                .sorted()
                .toList();
    }
}