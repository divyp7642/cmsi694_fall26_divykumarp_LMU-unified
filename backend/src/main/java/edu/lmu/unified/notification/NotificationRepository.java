
package edu.lmu.unified.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class NotificationRepository {

    private static final String NOTIFICATION_PREFIX =
            "lmu:notifications:";

    private static final String ORGANIZER_INDEX_PREFIX =
            "lmu:notifications:organizer:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public NotificationRepository(
            StringRedisTemplate redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    // Generate a unique notification ID.
    public String generateNotificationId() {
        return UUID.randomUUID().toString();
    }

    // Save a notification and associate it with its organizer.
    public void save(Notification notification) {
        try {
            String json =
                    objectMapper.writeValueAsString(notification);

            redisTemplate.opsForValue().set(
                    NOTIFICATION_PREFIX + notification.id(),
                    json
            );

            redisTemplate.opsForSet().add(
                    ORGANIZER_INDEX_PREFIX
                            + notification.organizerEmail(),
                    notification.id()
            );

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Unable to serialize notification",
                    exception
            );
        }
    }

    // Retrieve one notification by ID.
    public Optional<Notification> findById(String id) {
        String json = redisTemplate.opsForValue().get(
                NOTIFICATION_PREFIX + id
        );

        if (json == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(
                    objectMapper.readValue(
                            json,
                            Notification.class
                    )
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Unable to deserialize notification",
                    exception
            );
        }
    }

    // Retrieve notifications belonging to one organizer.
    public List<Notification> findByOrganizerEmail(
            String organizerEmail
    ) {
        var notificationIds =
                redisTemplate.opsForSet().members(
                        ORGANIZER_INDEX_PREFIX
                                + organizerEmail
                );

        if (notificationIds == null
                || notificationIds.isEmpty()) {
            return List.of();
        }

        return notificationIds.stream()
                .map(this::findById)
                .flatMap(Optional::stream)
                .filter(notification ->
                        organizerEmail.equals(
                                notification.organizerEmail()
                        )
                )
                .sorted(
                        Comparator.comparing(
                                Notification::createdAt
                        ).reversed()
                )
                .toList();
    }

    // SCRUM-24 - Mark a notification as read.
    // Only its owner can update it.
    public Optional<Notification> markAsRead(
            String notificationId,
            String organizerEmail
    ) {
        Optional<Notification> existing =
                findById(notificationId);

        if (existing.isEmpty()) {
            return Optional.empty();
        }

        Notification notification = existing.get();

        // Prevent one organizer from modifying another's notification.
        if (!organizerEmail.equals(
                notification.organizerEmail()
        )) {
            return Optional.empty();
        }

        // Already read: return without modifying Redis.
        if (notification.read()) {
            return Optional.of(notification);
        }

        Notification updated = new Notification(
                notification.id(),
                notification.eventId(),
                notification.eventTitle(),
                notification.organizerEmail(),
                notification.decision(),
                notification.message(),
                notification.createdAt(),
                true
        );

        save(updated);

        return Optional.of(updated);
    }

    // SCRUM-24 - Mark all notifications for one organizer as read.
    public int markAllAsRead(String organizerEmail) {
        List<Notification> notifications =
                findByOrganizerEmail(organizerEmail);

        int updatedCount = 0;

        for (Notification notification : notifications) {
            if (!notification.read()) {
                markAsRead(
                        notification.id(),
                        organizerEmail
                );
                updatedCount++;
            }
        }

        return updatedCount;
    }
}
