
package edu.lmu.unified.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventController eventController;

    private Event createEvent(
            Long id,
            String title,
            String status,
            String date
    ) {
        return new Event(
                id,
                title,
                "Test event description",
                date,
                "4:00 PM",
                "University Hall",
                "Technology",
                "organizer@lion.lmu.edu",
                status
        );
    }

    // Test 1 - Pending events must not appear publicly.
    @Test
    void shouldHidePendingEventsFromPublicList() {

        Event pendingEvent = createEvent(
                4L,
                "Pending Workshop",
                "PENDING_APPROVAL",
                LocalDate.now().plusDays(7).toString()
        );

        when(eventRepository.findAll())
                .thenReturn(List.of(pendingEvent));

        List<Event> events =
                eventController.getUpcomingEvents();

        assertFalse(
                events.stream().anyMatch(
                        event -> event.id().equals(4L)
                )
        );
    }

    // Test 2 - Pending event details must return 404.
    @Test
    void shouldHidePendingEventDetails() {

        Event pendingEvent = createEvent(
                4L,
                "Pending Workshop",
                "PENDING_APPROVAL",
                LocalDate.now().plusDays(7).toString()
        );

        when(eventRepository.findAll())
                .thenReturn(List.of(pendingEvent));

        ResponseEntity<Event> response =
                eventController.getEventById(4L);

        assertEquals(
                HttpStatus.NOT_FOUND,
                response.getStatusCode()
        );
    }

    // Test 3 - Approved future events should be visible.
    @Test
    void shouldShowApprovedUpcomingEvents() {

        Event approvedEvent = createEvent(
                5L,
                "Approved Workshop",
                "ACTIVE",
                LocalDate.now().plusDays(7).toString()
        );

        when(eventRepository.findAll())
                .thenReturn(List.of(approvedEvent));

        List<Event> events =
                eventController.getUpcomingEvents();

        assertTrue(
                events.stream().anyMatch(
                        event -> event.id().equals(5L)
                )
        );
    }

    // Test 4 - Approved events should have accessible details.
    @Test
    void shouldShowApprovedEventDetails() {

        Event approvedEvent = createEvent(
                5L,
                "Approved Workshop",
                "ACTIVE",
                LocalDate.now().plusDays(7).toString()
        );

        when(eventRepository.findAll())
                .thenReturn(List.of(approvedEvent));

        ResponseEntity<Event> response =
                eventController.getEventById(5L);

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertEquals(
                approvedEvent,
                response.getBody()
        );
    }

    // Test 5 - Past events must not appear in upcoming events.
    @Test
    void shouldHidePastEventsFromUpcomingList() {

        Event pastEvent = createEvent(
                6L,
                "Past Workshop",
                "ACTIVE",
                LocalDate.now().minusDays(7).toString()
        );

        when(eventRepository.findAll())
                .thenReturn(List.of(pastEvent));

        List<Event> events =
                eventController.getUpcomingEvents();

        assertFalse(
                events.stream().anyMatch(
                        event -> event.id().equals(6L)
                )
        );
    }

    // Test 6 - Public events must contain only ACTIVE events.
    @Test
    void shouldReturnOnlyActiveEvents() {

        Event pendingEvent = createEvent(
                4L,
                "Pending Workshop",
                "PENDING_APPROVAL",
                LocalDate.now().plusDays(7).toString()
        );

        Event approvedEvent = createEvent(
                5L,
                "Approved Workshop",
                "ACTIVE",
                LocalDate.now().plusDays(7).toString()
        );

        when(eventRepository.findAll())
                .thenReturn(List.of(
                        pendingEvent,
                        approvedEvent
                ));

        List<Event> events =
                eventController.getAllEvents();

        assertTrue(
                events.stream().allMatch(
                        event -> "ACTIVE".equals(event.status())
                )
        );

        assertTrue(
                events.stream().anyMatch(
                        event -> event.id().equals(5L)
                )
        );

        assertFalse(
                events.stream().anyMatch(
                        event -> event.id().equals(4L)
                )
        );
    }
}
