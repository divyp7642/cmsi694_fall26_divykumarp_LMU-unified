
package edu.lmu.unified.event;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RsvpRepository {

    private final StringRedisTemplate redisTemplate;

    public RsvpRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String getEventKey(Long eventId) {
        return "lmu:events:" + eventId + ":rsvps";
    }

    private String getStudentEmail(Rsvp rsvp) {
        return rsvp.studentEmail().trim().toLowerCase();
    }

    // Add RSVP - prevents duplicate registrations
    public boolean addRsvp(Rsvp rsvp) {
        String key = getEventKey(rsvp.eventId());

        Long added = redisTemplate.opsForSet().add(
                key,
                getStudentEmail(rsvp)
        );

        return added != null && added > 0;
    }

    // Cancel an existing RSVP
    public boolean cancelRsvp(Rsvp rsvp) {
        String key = getEventKey(rsvp.eventId());

        Long removed = redisTemplate.opsForSet().remove(
                key,
                getStudentEmail(rsvp)
        );

        return removed != null && removed > 0;
    }

    // Check whether a student has already registered
    public boolean hasRsvp(Rsvp rsvp) {
        String key = getEventKey(rsvp.eventId());

        Boolean exists = redisTemplate.opsForSet().isMember(
                key,
                getStudentEmail(rsvp)
        );

        return Boolean.TRUE.equals(exists);
    }

    // Count registered attendees
    public long getRsvpCount(Long eventId) {
        String key = getEventKey(eventId);

        Long count = redisTemplate.opsForSet().size(key);

        return count == null ? 0 : count;
    }
}
