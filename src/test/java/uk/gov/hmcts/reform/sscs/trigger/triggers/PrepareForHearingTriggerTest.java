package uk.gov.hmcts.reform.sscs.trigger.triggers;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseEventDetail;
import uk.gov.hmcts.reform.sscs.trigger.NightlyRunner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrepareForHearingTriggerTest {

    private static final String CASE_ID = "1234";

    @Mock
    private NightlyRunner nightlyRunner;

    private Trigger trigger;
    private LocalDate today;

    @BeforeEach
    void setup() {
        today = LocalDate.now();
        trigger = new PrepareForHearingTrigger(nightlyRunner, today);
    }

    @Test
    void shouldReturnValidQuery() {
        final String dateFormat = "yyyy-MM-dd";
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateFormat);

        String query = trigger.query();

        JSONObject result = new JSONObject(query);
        assertThat(result.query("/fields/0")).isEqualTo("reference");
        assertThat(result.query("/_source")).isEqualTo(false);
        assertThat(result.query("/query/match/data.hearings.value.hearingDate"))
            .isEqualTo(dateTimeFormatter.format(today.plusDays(14)));
    }

    @Test
    void shouldReturnsCorrectEvent() {
        assertThat(trigger.event()).isEqualTo("prepareForHearing");
    }

    @Test
    void isValidShouldReturnTrueWhenEventNotPreviouslyTriggered() {
        List<CaseEventDetail> events = List.of(
            CaseEventDetail.builder().id("someEvent").createdDate(LocalDateTime.now()).build()
        );

        assertTrue(trigger.isValid(events), "isValid should return true");
    }

    @Test
    void isValidShouldReturnFalseWhenEventAlreadyTriggered() {
        List<CaseEventDetail> events = List.of(
            CaseEventDetail.builder().id("someEvent").createdDate(LocalDateTime.now()).build(),
            CaseEventDetail.builder().id("prepareForHearing").createdDate(LocalDateTime.now()).build()
        );

        assertFalse(trigger.isValid(events), "isValid should return false");
    }

    @Test
    void processCaseShouldCallNightlyRunnerWhenEventIsValid() {
        // Given
        List<CaseEventDetail> events = List.of(
            CaseEventDetail.builder().id("someEvent").createdDate(LocalDateTime.now()).build()
        );
        when(nightlyRunner.getCaseEvents(CASE_ID)).thenReturn(events);

        // When
        trigger.processCase(CASE_ID);

        // Then
        verify(nightlyRunner, times(1)).getCaseEvents(CASE_ID);
        verify(nightlyRunner, times(1)).processCase(CASE_ID, "prepareForHearing");
    }

    @Test
    void processCaseShouldNotCallNightlyRunnerWhenEventIsInvalid() {
        // Given
        List<CaseEventDetail> events = List.of(
            CaseEventDetail.builder().id("prepareForHearing").createdDate(LocalDateTime.now()).build()
        );
        when(nightlyRunner.getCaseEvents(CASE_ID)).thenReturn(events);

        // When
        trigger.processCase(CASE_ID);

        // Then
        verify(nightlyRunner, times(1)).getCaseEvents(CASE_ID);
        verify(nightlyRunner, times(0)).processCase(CASE_ID, "prepareForHearing");
    }
}
