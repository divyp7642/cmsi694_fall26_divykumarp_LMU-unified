
package edu.lmu.unified.event;

import org.springframework.stereotype.Service;

@Service
public class RsvpService {

    private final RsvpRepository repository;

    public RsvpService(RsvpRepository repository) {
        this.repository = repository;
    }

    public boolean addRsvp(Long eventId, String studentEmail) {
        Rsvp rsvp = new Rsvp(
                eventId,
                studentEmail.trim().toLowerCase()
        );

        return repository.addRsvp(rsvp);
    }

    public boolean cancelRsvp(Long eventId, String studentEmail) {
        Rsvp rsvp = new Rsvp(
                eventId,
                studentEmail.trim().toLowerCase()
        );

        return repository.cancelRsvp(rsvp);
    }

    public boolean hasRsvp(Long eventId, String studentEmail) {
        Rsvp rsvp = new Rsvp(
                eventId,
                studentEmail.trim().toLowerCase()
        );

        return repository.hasRsvp(rsvp);
    }

    public long getRsvpCount(Long eventId) {
        return repository.getRsvpCount(eventId);
    }
}
