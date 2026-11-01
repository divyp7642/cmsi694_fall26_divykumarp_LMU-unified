
package edu.lmu.unified.event;

import org.springframework.stereotype.Service;

@Service
public class EventSubmissionService {

    private final EventRepository eventRepository;

    public EventSubmissionService(
            EventRepository eventRepository
    ) {
        this.eventRepository = eventRepository;
    }

    public Event submitEvent(
            CreateEventRequest request,
            String organizerEmail
    ) {
        if (organizerEmail == null ||
                organizerEmail.isBlank()) {
            throw new IllegalArgumentException(
                    "Organizer email is required"
            );
        }

        Long eventId = eventRepository.generateEventId();

        Event event = new Event(
                eventId,
                request.title().trim(),
                request.description().trim(),
                request.date().toString(),
                request.time().trim(),
                request.location().trim(),
                request.category().trim(),
                organizerEmail.trim().toLowerCase(),
                "PENDING_APPROVAL"
        );

        eventRepository.save(event);

        return event;
    }
}
