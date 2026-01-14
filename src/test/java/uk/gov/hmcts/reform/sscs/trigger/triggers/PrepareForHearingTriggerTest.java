package uk.gov.hmcts.reform.sscs.trigger.triggers;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.trigger.NightlyRunner;

import static org.assertj.core.api.Assertions.assertThat;

class PrepareForHearingTriggerTest {

    @Mock
    private NightlyRunner nightlyRunner;

    private Trigger trigger;

    @BeforeEach
    void setup() {
        trigger = new PrepareForHearingTrigger(nightlyRunner, LocalDate.now());
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
        assertThat(result.query("/query/match/data.hearings.value.hearingDate"))
            .isEqualTo(dateTimeFormatter.format(today.plusDays(14)));
    }

    @Test
    void shouldReturnsCorrectEvent() {
        assertThat(trigger.event()).isEqualTo("prepareForHearing");
    }
}
