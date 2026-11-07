
package edu.lmu.unified.notification;

public record Notification(
        String id,
        Long eventId,
        String eventTitle,
        String organizerEmail,
        String decision,
        String message,
        String createdAt,
        boolean read
) {
}
