package edu.lmu.unified.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SavedEventServiceTest {

    private SavedEventRepository repository;

    private EventController eventController;

    private SavedEventService service;

    private static final String STUDENT_EMAIL =
            "student@lion.lmu.edu";

    @BeforeEach
    void setUp() {

        repository = mock(SavedEventRepository.class);

        eventController = mock(EventController.class);

        service = new SavedEventService(
                repository,
                eventController
        );
    }

    private Event createEvent(Long id, String status) {

        return new Event(
                id,
                "Test Campus Event",
                "Test event description",
                "2026-12-15",
                "2:00 PM",
                "University Hall",
                "Technology",
                "LMU Tech Club",
                status
        );
    }

    // Test 1 - Save a predefined active event.
    @Test
    void shouldSaveActiveEvent() {

        when(eventController.getAllEvents())
                .thenReturn(List.of(
                        createEvent(1L, "ACTIVE")
                ));

        when(repository.saveEvent(1L, STUDENT_EMAIL))
                .thenReturn(true);

        assertTrue(
                service.saveEvent(1L, STUDENT_EMAIL)
        );

        verify(repository)
                .saveEvent(1L, STUDENT_EMAIL);
    }

    // Test 2 - Save an approved organizer-submitted event.
    @Test
    void shouldSaveApprovedEvent() {

        when(eventController.getAllEvents())
                .thenReturn(List.of(
                        createEvent(5L, "APPROVED")
                ));

        when(repository.saveEvent(5L, STUDENT_EMAIL))
                .thenReturn(true);

        assertTrue(
                service.saveEvent(5L, STUDENT_EMAIL)
        );

        verify(repository)
                .saveEvent(5L, STUDENT_EMAIL);
    }

    // Test 3 - Handle an already saved event.
    @Test
    void shouldHandleDuplicateSavedEvent() {

        when(eventController.getAllEvents())
                .thenReturn(List.of(
                        createEvent(1L, "ACTIVE")
                ));

        when(repository.saveEvent(1L, STUDENT_EMAIL))
                .thenReturn(false);

        assertFalse(
                service.saveEvent(1L, STUDENT_EMAIL)
        );

        verify(repository)
                .saveEvent(1L, STUDENT_EMAIL);
    }

    // Test 4 - Reject a nonexistent event.
    @Test
    void shouldRejectNonexistentEvent() {

        when(eventController.getAllEvents())
                .thenReturn(List.of(
                        createEvent(1L, "ACTIVE")
                ));

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.saveEvent(
                                999L,
                                STUDENT_EMAIL
                        )
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        assertEquals(
                "Event is unavailable or does not exist.",
                exception.getReason()
        );

        verifyNoInteractions(repository);
    }

    // Test 5 - Reject an event excluded from public discovery.
    @Test
    void shouldRejectUnavailableEvent() {

        when(eventController.getAllEvents())
                .thenReturn(List.of());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.saveEvent(
                                5L,
                                STUDENT_EMAIL
                        )
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verifyNoInteractions(repository);
    }

    // Test 6 - Remove a saved event.
    @Test
    void shouldRemoveSavedEvent() {

        when(repository.removeSavedEvent(
                1L,
                STUDENT_EMAIL
        )).thenReturn(true);

        assertTrue(
                service.removeSavedEvent(
                        1L,
                        STUDENT_EMAIL
                )
        );

        verify(repository)
                .removeSavedEvent(
                        1L,
                        STUDENT_EMAIL
                );
    }

    // Test 7 - Handle removal of an unsaved event.
    @Test
    void shouldHandleRemovalOfUnsavedEvent() {

        when(repository.removeSavedEvent(
                1L,
                STUDENT_EMAIL
        )).thenReturn(false);

        assertFalse(
                service.removeSavedEvent(
                        1L,
                        STUDENT_EMAIL
                )
        );

        verify(repository)
                .removeSavedEvent(
                        1L,
                        STUDENT_EMAIL
                );
    }

    // Test 8 - Allow removal even if an event is unavailable.
    @Test
    void shouldRemovePreviouslySavedUnavailableEvent() {

        when(repository.removeSavedEvent(
                5L,
                STUDENT_EMAIL
        )).thenReturn(true);

        assertTrue(
                service.removeSavedEvent(
                        5L,
                        STUDENT_EMAIL
                )
        );

        verify(repository)
                .removeSavedEvent(
                        5L,
                        STUDENT_EMAIL
                );

        // Removing saved events must not require
        // the event to remain publicly available.
        verifyNoInteractions(eventController);
    }

    // Test 9 - Check saved-event status.
    @Test
    void shouldReturnTrueForSavedEvent() {

        when(repository.isEventSaved(
                1L,
                STUDENT_EMAIL
        )).thenReturn(true);

        assertTrue(
                service.isEventSaved(
                        1L,
                        STUDENT_EMAIL
                )
        );

        verify(repository)
                .isEventSaved(
                        1L,
                        STUDENT_EMAIL
                );
    }

    // Test 10 - Check unsaved-event status.
    @Test
    void shouldReturnFalseForUnsavedEvent() {

        when(repository.isEventSaved(
                1L,
                STUDENT_EMAIL
        )).thenReturn(false);

        assertFalse(
                service.isEventSaved(
                        1L,
                        STUDENT_EMAIL
                )
        );

        verify(repository)
                .isEventSaved(
                        1L,
                        STUDENT_EMAIL
                );
    }

    // Test 11 - Retrieve saved event IDs.
    @Test
    void shouldRetrieveSavedEventIds() {

        when(repository.getSavedEventIds(STUDENT_EMAIL))
                .thenReturn(List.of(1L, 3L, 5L));

        assertEquals(
                List.of(1L, 3L, 5L),
                service.getSavedEventIds(STUDENT_EMAIL)
        );

        verify(repository)
                .getSavedEventIds(STUDENT_EMAIL);
    }

    // Test 12 - Retrieve an empty saved-event list.
    @Test
    void shouldReturnEmptySavedEventList() {

        when(repository.getSavedEventIds(STUDENT_EMAIL))
                .thenReturn(List.of());

        List<Long> savedEventIds =
                service.getSavedEventIds(STUDENT_EMAIL);

        assertNotNull(savedEventIds);

        assertTrue(savedEventIds.isEmpty());

        verify(repository)
                .getSavedEventIds(STUDENT_EMAIL);
    }
}