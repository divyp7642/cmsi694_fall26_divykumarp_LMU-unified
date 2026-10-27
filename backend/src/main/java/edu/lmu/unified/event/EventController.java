
package edu.lmu.unified.event;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "http://localhost:5173")
public class EventController {

    private final List<Event> events = List.of(
            new Event(
                    1L,
                    "LMU Career Fair",
                    "Meet employers and explore career opportunities.",
                    "2026-10-10",
                    "2:00 PM",
                    "Burns Recreation Center",
                    "Career",
                    "LMU Career and Professional Development",
                    "ACTIVE"
            ),

            new Event(
                    2L,
                    "Tech Club Meetup",
                    "Connect with students interested in technology and software development.",
                    "2026-10-15",
                    "5:00 PM",
                    "University Hall",
                    "Technology",
                    "LMU Tech Club",
                    "ACTIVE"
            ),

            new Event(
                    3L,
                    "LMU Movie Night",
                    "Enjoy a movie night with fellow LMU students.",
                    "2026-10-18",
                    "7:00 PM",
                    "Sunken Garden",
                    "Entertainment",
                    "LMU Student Activities",
                    "ACTIVE"
            )
    );

    // SCRUM-15 - Browse upcoming events
    @GetMapping
    public List<Event> getUpcomingEvents() {
        return events.stream()
                .filter(event ->
                        !LocalDate.parse(event.date())
                                .isBefore(LocalDate.now())
                )
                .toList();
    }

    // SCRUM-17 - Get details for one event
    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(
            @PathVariable Long id
    ) {
        return events.stream()
                .filter(event -> event.id().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
