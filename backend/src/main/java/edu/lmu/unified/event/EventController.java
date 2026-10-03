package edu.lmu.unified.event;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "http://localhost:5173")
public class EventController {

    @GetMapping
    public List<Event> getUpcomingEvents() {

        return List.of(
                new Event(
                        1L,
                        "LMU Career Fair",
                        "Meet employers and explore career opportunities.",
                        "2026-10-10",
                        "2:00 PM",
                        "Burns Recreation Center",
                        "Career"
                ),

                new Event(
                        2L,
                        "Tech Club Meetup",
                        "Connect with students interested in technology and software development.",
                        "2026-10-15",
                        "5:00 PM",
                        "University Hall",
                        "Technology"
                ),

                new Event(
                        3L,
                        "LMU Movie Night",
                        "Enjoy a movie night with fellow LMU students.",
                        "2026-10-18",
                        "7:00 PM",
                        "Sunken Garden",
                        "Entertainment"
                )
        );
    }
}