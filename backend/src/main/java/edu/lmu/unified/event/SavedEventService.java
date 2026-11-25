package edu.lmu.unified.event;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SavedEventService {

    private final SavedEventRepository repository;
    private final EventController eventController;

    public SavedEventService(
            SavedEventRepository repository,
            EventController eventController
    ) {
        this.repository = repository;
        this.eventController = eventController;
    }

    // SCRUM-28 - Save a publicly available event.
    public boolean saveEvent(
            Long eventId,
            String studentEmail
    ) {
        validatePublicEvent(eventId);

        return repository.saveEvent(
                eventId,
                studentEmail
        );
    }

    // SCRUM-28 - Remove an event from saved events.
    public boolean removeSavedEvent(
            Long eventId,
            String studentEmail
    ) {
        return repository.removeSavedEvent(
                eventId,
                studentEmail
        );
    }

    // SCRUM-28 - Check whether an event is saved.
    public boolean isEventSaved(
            Long eventId,
            String studentEmail
    ) {
        return repository.isEventSaved(
                eventId,
                studentEmail
        );
    }

    // SCRUM-28 - Retrieve saved event IDs.
    public List<Long> getSavedEventIds(
            String studentEmail
    ) {
        return repository.getSavedEventIds(
                studentEmail
        );
    }

    // Validate that an event exists and is publicly available.
    private void validatePublicEvent(Long eventId) {
        boolean exists = eventController.getAllEvents()
                .stream()
                .anyMatch(event ->
                        event.id().equals(eventId)
                );

        if (!exists) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Event is unavailable or does not exist."
            );
        }
    }
}