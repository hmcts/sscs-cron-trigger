package uk.gov.hmcts.reform.sscs.trigger.triggers;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.ccd.client.model.CaseEventDetail;
import uk.gov.hmcts.reform.ccd.client.model.Event;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectionDueTodayTriggerTest {

    private Trigger trigger;

    @BeforeEach
    void setup() {
        trigger = new DirectionDueTodayTrigger(LocalDate.now());
    }

    @Test
    void shouldReturnValidQuery() {
        final String dateFormat = "yyyy-MM-dd";
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateFormat);
        final LocalDate today = LocalDate.now();

        String query = trigger.query();

        JSONObject result = new JSONObject(query);
        assertThat(result.query("/fields/0"))
            .isEqualTo("reference");
        assertThat(result.query("/_source"))
            .isEqualTo(false);
        assertThat(result.query("/query/match/data.directionDueDate"))
            .isEqualTo(dateTimeFormatter.format(today));
    }

    @Test
    void isValidShouldReturnTrueWhenEventNotPreviouslyTriggered() {
        List<CaseEventDetail> events = Arrays.asList(
            CaseEventDetail.builder().id("someEvent").createdDate(LocalDateTime.now()).build()
        );

        assertTrue(trigger.isValid(events), "isValid should return true");
    }

    @Test
    void isValidShouldReturnFalseWhenEventAlreadyTriggered() {
        List<CaseEventDetail> events = Arrays.asList(
            CaseEventDetail.builder().id("someEvent").createdDate(LocalDateTime.now()).build(),
            CaseEventDetail.builder().id("directionDueToday").createdDate(LocalDateTime.now()).build()
        );

        assertFalse(trigger.isValid(events), "isValid should return false");
    }

    @Test
    void shouldReturnsCorrectEvent() {
        Event event = trigger.event();

        assertThat(event.getId()).isEqualTo("directionDueToday");
    }
}
