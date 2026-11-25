package edu.lmu.unified.student;

import edu.lmu.unified.auth.AuthorizationService;
import edu.lmu.unified.auth.UserRole;
import edu.lmu.unified.event.SavedEventService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SavedEventControllerTest {

    private AuthorizationService authorizationService;

    private SavedEventService savedEventService;

    private MockMvc mockMvc;

    private static final String BASE_URL =
            "/api/student/saved-events";

    private static final String TOKEN =
            "Bearer student-test-token";

    private static final String STUDENT_EMAIL =
            "student@lion.lmu.edu";

    private static final String OTHER_STUDENT_EMAIL =
            "another-student@lion.lmu.edu";

    @BeforeEach
    void setUp() {

        authorizationService =
                mock(AuthorizationService.class);

        savedEventService =
                mock(SavedEventService.class);

        SavedEventController controller =
                new SavedEventController(
                        authorizationService,
                        savedEventService
                );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    private void mockAuthenticatedStudent() {

        when(authorizationService
                .getAuthenticatedEmail(TOKEN))
                .thenReturn(
                        Optional.of(STUDENT_EMAIL)
                );

        when(authorizationService.hasRole(
                TOKEN,
                UserRole.STUDENT
        )).thenReturn(true);
    }

    private void mockUnauthenticatedUser() {

        when(authorizationService
                .getAuthenticatedEmail(null))
                .thenReturn(Optional.empty());
    }

    private void mockNonStudentUser() {

        when(authorizationService
                .getAuthenticatedEmail(TOKEN))
                .thenReturn(
                        Optional.of(
                                "organizer@lion.lmu.edu"
                        )
                );

        when(authorizationService.hasRole(
                TOKEN,
                UserRole.STUDENT
        )).thenReturn(false);
    }

    // Test 1 - Successfully save an event.
    @Test
    void shouldSaveEventSuccessfully()
            throws Exception {

        mockAuthenticatedStudent();

        when(savedEventService.saveEvent(
                1L,
                STUDENT_EMAIL
        )).thenReturn(true);

        mockMvc.perform(
                post(BASE_URL + "/1")
                        .header("Authorization", TOKEN)
        )
        .andExpect(status().isCreated())
        .andExpect(
                jsonPath("$.message")
                        .value("Event saved successfully.")
        )
        .andExpect(jsonPath("$.eventId").value(1))
        .andExpect(jsonPath("$.saved").value(true));

        verify(savedEventService)
                .saveEvent(1L, STUDENT_EMAIL);
    }

    // Test 2 - Handle duplicate save requests.
    @Test
    void shouldHandleDuplicateSavedEvent()
            throws Exception {

        mockAuthenticatedStudent();

        when(savedEventService.saveEvent(
                1L,
                STUDENT_EMAIL
        )).thenReturn(false);

        mockMvc.perform(
                post(BASE_URL + "/1")
                        .header("Authorization", TOKEN)
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.message")
                        .value("Event is already saved.")
        )
        .andExpect(jsonPath("$.eventId").value(1))
        .andExpect(jsonPath("$.saved").value(true));

        verify(savedEventService)
                .saveEvent(1L, STUDENT_EMAIL);
    }

    // Test 3 - Successfully remove a saved event.
    @Test
    void shouldRemoveSavedEventSuccessfully()
            throws Exception {

        mockAuthenticatedStudent();

        when(savedEventService.removeSavedEvent(
                1L,
                STUDENT_EMAIL
        )).thenReturn(true);

        mockMvc.perform(
                delete(BASE_URL + "/1")
                        .header("Authorization", TOKEN)
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.message")
                        .value(
                                "Saved event removed successfully."
                        )
        )
        .andExpect(jsonPath("$.eventId").value(1))
        .andExpect(jsonPath("$.saved").value(false));

        verify(savedEventService)
                .removeSavedEvent(1L, STUDENT_EMAIL);
    }

    // Test 4 - Handle removal of an unsaved event.
    @Test
    void shouldHandleRemovalOfUnsavedEvent()
            throws Exception {

        mockAuthenticatedStudent();

        when(savedEventService.removeSavedEvent(
                1L,
                STUDENT_EMAIL
        )).thenReturn(false);

        mockMvc.perform(
                delete(BASE_URL + "/1")
                        .header("Authorization", TOKEN)
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.message")
                        .value(
                                "Event is not in your saved events."
                        )
        )
        .andExpect(jsonPath("$.eventId").value(1))
        .andExpect(jsonPath("$.saved").value(false));

        verify(savedEventService)
                .removeSavedEvent(1L, STUDENT_EMAIL);
    }

    // Test 5 - Retrieve saved event IDs.
    @Test
    void shouldRetrieveSavedEventIds()
            throws Exception {

        mockAuthenticatedStudent();

        when(savedEventService.getSavedEventIds(
                STUDENT_EMAIL
        )).thenReturn(List.of(1L, 3L, 5L));

        mockMvc.perform(
                get(BASE_URL + "/ids")
                        .header("Authorization", TOKEN)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0]").value(1))
        .andExpect(jsonPath("$[1]").value(3))
        .andExpect(jsonPath("$[2]").value(5));

        verify(savedEventService)
                .getSavedEventIds(STUDENT_EMAIL);
    }

    // Test 6 - Retrieve an empty saved-event list.
    @Test
    void shouldReturnEmptySavedEventList()
            throws Exception {

        mockAuthenticatedStudent();

        when(savedEventService.getSavedEventIds(
                STUDENT_EMAIL
        )).thenReturn(List.of());

        mockMvc.perform(
                get(BASE_URL + "/ids")
                        .header("Authorization", TOKEN)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

        verify(savedEventService)
                .getSavedEventIds(STUDENT_EMAIL);
    }

    // Test 7 - Reject unauthenticated save requests.
    @Test
    void shouldRejectUnauthenticatedSave()
            throws Exception {

        mockUnauthenticatedUser();

        mockMvc.perform(
                post(BASE_URL + "/1")
        )
        .andExpect(status().isUnauthorized())
        .andExpect(
                jsonPath("$.message")
                        .value("Please log in first.")
        );

        verifyNoInteractions(savedEventService);
    }

    // Test 8 - Reject unauthenticated removal requests.
    @Test
    void shouldRejectUnauthenticatedRemoval()
            throws Exception {

        mockUnauthenticatedUser();

        mockMvc.perform(
                delete(BASE_URL + "/1")
        )
        .andExpect(status().isUnauthorized())
        .andExpect(
                jsonPath("$.message")
                        .value("Please log in first.")
        );

        verifyNoInteractions(savedEventService);
    }

    // Test 9 - Reject unauthenticated retrieval requests.
    @Test
    void shouldRejectUnauthenticatedRetrieval()
            throws Exception {

        mockUnauthenticatedUser();

        mockMvc.perform(
                get(BASE_URL + "/ids")
        )
        .andExpect(status().isUnauthorized())
        .andExpect(
                jsonPath("$.message")
                        .value("Please log in first.")
        );

        verifyNoInteractions(savedEventService);
    }

    // Test 10 - Reject an invalid or expired token.
    @Test
    void shouldRejectInvalidToken()
            throws Exception {

        String invalidToken =
                "Bearer invalid-token";

        when(authorizationService
                .getAuthenticatedEmail(invalidToken))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                post(BASE_URL + "/1")
                        .header(
                                "Authorization",
                                invalidToken
                        )
        )
        .andExpect(status().isUnauthorized())
        .andExpect(
                jsonPath("$.message")
                        .value("Please log in first.")
        );

        verifyNoInteractions(savedEventService);
    }

    // Test 11 - Reject non-student save requests.
    @Test
    void shouldRejectNonStudentSave()
            throws Exception {

        mockNonStudentUser();

        mockMvc.perform(
                post(BASE_URL + "/1")
                        .header("Authorization", TOKEN)
        )
        .andExpect(status().isForbidden())
        .andExpect(
                jsonPath("$.message")
                        .value(
                                "Access denied. Student role required."
                        )
        );

        verifyNoInteractions(savedEventService);
    }

    // Test 12 - Reject non-student removal requests.
    @Test
    void shouldRejectNonStudentRemoval()
            throws Exception {

        mockNonStudentUser();

        mockMvc.perform(
                delete(BASE_URL + "/1")
                        .header("Authorization", TOKEN)
        )
        .andExpect(status().isForbidden())
        .andExpect(
                jsonPath("$.message")
                        .value(
                                "Access denied. Student role required."
                        )
        );

        verifyNoInteractions(savedEventService);
    }

    // Test 13 - Reject non-student retrieval requests.
    @Test
    void shouldRejectNonStudentRetrieval()
            throws Exception {

        mockNonStudentUser();

        mockMvc.perform(
                get(BASE_URL + "/ids")
                        .header("Authorization", TOKEN)
        )
        .andExpect(status().isForbidden())
        .andExpect(
                jsonPath("$.message")
                        .value(
                                "Access denied. Student role required."
                        )
        );

        verifyNoInteractions(savedEventService);
    }

    // Test 14 - Reject unavailable or nonexistent events.
    @Test
    void shouldRejectUnavailableEvent()
            throws Exception {

        mockAuthenticatedStudent();

        when(savedEventService.saveEvent(
                999L,
                STUDENT_EMAIL
        )).thenThrow(
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Event is unavailable or does not exist."
                )
        );

        mockMvc.perform(
                post(BASE_URL + "/999")
                        .header("Authorization", TOKEN)
        )
        .andExpect(status().isNotFound())
        .andExpect(
                jsonPath("$.message")
                        .value(
                                "Event is unavailable or does not exist."
                        )
        );

        verify(savedEventService)
                .saveEvent(999L, STUDENT_EMAIL);
    }

    // Test 15 - Saving must use authenticated identity.
    @Test
    void shouldUseAuthenticatedIdentityWhenSaving()
            throws Exception {

        mockAuthenticatedStudent();

        when(savedEventService.saveEvent(
                1L,
                STUDENT_EMAIL
        )).thenReturn(true);

        mockMvc.perform(
                post(BASE_URL + "/1")
                        .header("Authorization", TOKEN)
                        .param(
                                "studentEmail",
                                OTHER_STUDENT_EMAIL
                        )
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.saved").value(true));

        verify(savedEventService)
                .saveEvent(1L, STUDENT_EMAIL);

        verify(savedEventService, never())
                .saveEvent(
                        1L,
                        OTHER_STUDENT_EMAIL
                );
    }

    // Test 16 - Removal must use authenticated identity.
    @Test
    void shouldUseAuthenticatedIdentityWhenRemoving()
            throws Exception {

        mockAuthenticatedStudent();

        when(savedEventService.removeSavedEvent(
                1L,
                STUDENT_EMAIL
        )).thenReturn(false);

        mockMvc.perform(
                delete(BASE_URL + "/1")
                        .header("Authorization", TOKEN)
                        .param(
                                "studentEmail",
                                OTHER_STUDENT_EMAIL
                        )
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.saved").value(false));

        verify(savedEventService)
                .removeSavedEvent(
                        1L,
                        STUDENT_EMAIL
                );

        verify(savedEventService, never())
                .removeSavedEvent(
                        1L,
                        OTHER_STUDENT_EMAIL
                );
    }

    // Test 17 - Retrieval must use authenticated identity.
    @Test
    void shouldUseAuthenticatedIdentityWhenRetrieving()
            throws Exception {

        mockAuthenticatedStudent();

        when(savedEventService.getSavedEventIds(
                STUDENT_EMAIL
        )).thenReturn(List.of(1L));

        mockMvc.perform(
                get(BASE_URL + "/ids")
                        .header("Authorization", TOKEN)
                        .param(
                                "studentEmail",
                                OTHER_STUDENT_EMAIL
                        )
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0]").value(1));

        verify(savedEventService)
                .getSavedEventIds(STUDENT_EMAIL);

        verify(savedEventService, never())
                .getSavedEventIds(
                        OTHER_STUDENT_EMAIL
                );
    }

    // Test 18 - Reject an invalid event ID format.
    @Test
    void shouldRejectInvalidEventIdFormat()
            throws Exception {

        mockMvc.perform(
                post(BASE_URL + "/invalid-id")
                        .header("Authorization", TOKEN)
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(savedEventService);
    }
}