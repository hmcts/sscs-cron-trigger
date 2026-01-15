package uk.gov.hmcts.reform.sscs.trigger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.trigger.triggers.Trigger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NightlyRunnerAppTest {

    @Mock
    private NightlyRunner nightlyRunner;
    @Mock
    private Trigger trigger1;
    @Mock
    private Trigger trigger2;

    private NightlyRunnerApp nightlyRunnerApp;

    @BeforeEach
    void setUp() {
        List<Trigger> triggers = new ArrayList<>();
        triggers.add(trigger1);
        triggers.add(trigger2);

        nightlyRunnerApp = new NightlyRunnerApp(nightlyRunner, triggers);
    }

    @Test
    void shouldRejectNullTriggers() {
        assertThrows(NullPointerException.class, () -> new NightlyRunnerApp(nightlyRunner, null));
    }

    @Test
    void shouldProcessAllTriggersAndCases() {
        // Given
        String query1 = "query1";
        String query2 = "query2";
        when(trigger1.query()).thenReturn(query1);
        when(trigger2.query()).thenReturn(query2);
        when(nightlyRunner.findCases(query1)).thenReturn(List.of(CaseDetails.builder().id(1L).build()));
        when(nightlyRunner.findCases(query2)).thenReturn(List.of(CaseDetails.builder().id(2L).build()));

        // When
        nightlyRunnerApp.run();

        // Then
        verify(trigger1, times(1)).query();
        verify(trigger2, times(1)).query();
        verify(nightlyRunner, times(1)).findCases(query1);
        verify(nightlyRunner, times(1)).findCases(query2);
        verify(trigger1, times(1)).processCase("1");
        verify(trigger2, times(1)).processCase("2");
    }

    @Test
    void shouldHandleEmptyCaseList() {
        // Given
        String query = "query";
        when(trigger1.query()).thenReturn(query);
        when(nightlyRunner.findCases(query)).thenReturn(Collections.emptyList());

        // When
        nightlyRunnerApp.run();

        // Then
        verify(trigger1, times(1)).query();
        verify(nightlyRunner, times(1)).findCases(query);
        verify(trigger1, never()).processCase(anyString());
    }

    @Test
    void shouldHandleExceptionInTriggerQuery() {
        // Given
        when(trigger1.query()).thenThrow(new RuntimeException("Test exception"));

        // When/Then
        assertDoesNotThrow(() -> nightlyRunnerApp.run());
        verify(trigger2, times(1)).query();
    }

    @Test
    void shouldHandleExceptionInFindCases() {
        // Given
        String query = "query";
        when(trigger1.query()).thenReturn(query);
        when(nightlyRunner.findCases(query)).thenThrow(new RuntimeException("Test exception"));

        // When/Then
        assertDoesNotThrow(() -> nightlyRunnerApp.run());
        verify(trigger2, times(1)).query();
    }

    @Test
    void shouldHandleExceptionInProcessCase() {
        // Given
        String query = "query";
        when(trigger1.query()).thenReturn(query);
        when(nightlyRunner.findCases(query)).thenReturn(List.of(CaseDetails.builder().id(1L).build()));
        doThrow(new RuntimeException("Test exception")).when(trigger1).processCase("1");

        // When/Then
        assertDoesNotThrow(() -> nightlyRunnerApp.run());
        verify(trigger2, times(1)).query();
    }
}
