package uk.gov.hmcts.reform.sscs.trigger.triggers;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.ccd.client.model.Event;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

class HearingTodayTriggerTest {

    private Trigger trigger;

    @BeforeEach
    void setup() {
        trigger = new HearingTodayTrigger(LocalDate.now());
    }

    @Test
    void shouldReturnValidQuery() {
        final String dateFormat = "yyyy-MM-dd";
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateFormat);
        final LocalDate today = LocalDate.now();

        String query = trigger.query();

        JSONObject result = new JSONObject(query);
        assertThat(result.query("/fields/0")).isEqualTo("reference");
        assertThat(result.query("/_source")).isEqualTo(false);
        assertThat(result.query("/query/bool/filter/0/range/data.hearings.value.hearingDate/gte"))
            .isEqualTo(dateTimeFormatter.format(today));
        assertThat(result.query("/query/bool/filter/0/range/data.hearings.value.hearingDate/lte"))
            .isEqualTo(dateTimeFormatter.format(today.plusDays(1)));
        assertThat(result.query("/query/bool/filter/0/range/data.hearings.value.hearingDate/format"))
            .isEqualTo("yyyy-MM-dd");
    }

    @Test
    void shouldReturnsCorrectEvent() {
        Event event = trigger.event();

        assertThat(event.getId()).isEqualTo("hearingToday");
    }
}
