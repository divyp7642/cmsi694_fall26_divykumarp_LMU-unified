
package edu.lmu.unified.notification;

import edu.lmu.unified.event.Event;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(
            NotificationRepository notificationRepository
    ) {
        this.notificationRepository = notificationRepository;
    }

    // SCRUM-24 - Create a notification for an approval decision.
    public Notification createDecisionNotification(Event event) {

        if (!"APPROVED".equals(event.status())
                && !"REJECTED".equals(event.status())) {
            throw new IllegalArgumentException(
                    "Notifications require an approved or rejected event."
            );
        }

        String message = "APPROVED".equals(event.status())
                ? "Your event \"" + event.title()
                    + "\" has been approved and is now published."
                : "Your event \"" + event.title()
                    + "\" has been rejected by an administrator.";

        Notification notification = new Notification(
                notificationRepository.generateNotificationId(),
                event.id(),
                event.title(),
                event.organizer(),
                event.status(),
                message,
                Instant.now().toString(),
                false
        );

        notificationRepository.save(notification);

        return notification;
    }

    // SCRUM-24 - Retrieve notifications for the logged-in organizer.
    public List<Notification> getOrganizerNotifications(
            String organizerEmail
    ) {
        return notificationRepository.findByOrganizerEmail(
                organizerEmail
        );
    }

    // SCRUM-24 - Mark one notification as read.
    // The repository verifies notification ownership.
    public Optional<Notification> markAsRead(
            String notificationId,
            String organizerEmail
    ) {
        return notificationRepository.markAsRead(
                notificationId,
                organizerEmail
        );
    }

    // SCRUM-24 - Mark all of an organizer's notifications as read.
    public int markAllAsRead(String organizerEmail) {
        return notificationRepository.markAllAsRead(
                organizerEmail
        );
    }
}
