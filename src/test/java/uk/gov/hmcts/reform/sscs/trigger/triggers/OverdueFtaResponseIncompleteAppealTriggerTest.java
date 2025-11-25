
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

class OverdueFtaResponseIncompleteAppealTriggerTest {

    private Trigger trigger;

    @BeforeEach
    void setup() {
        trigger = new OverdueFtaResponseIncompleteAppealTrigger(LocalDate.now());
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
        assertThat(result.query("/query/bool/must/0/match/data.ftaCommunications.value.requestDateTime"))
            .isEqualTo(dateTimeFormatter.format(today.minusDays(2)));
        assertThat(result.query("/query/bool/must/1/match/state"))
            .isEqualTo("incompleteApplication");
        assertThat(result.query("/query/bool/must_not/0/exists/field"))
            .isEqualTo("data.ftaCommunications.value.requestReply");
    }

    @Test
    void isValidShouldReturnTrue() {
        List<CaseEventDetail> events = Arrays.asList(
            CaseEventDetail.builder().id("someEvent").createdDate(LocalDateTime.now()).build()
        );
        assertThat(trigger.isValid(events)).isTrue();
    }

    @Test
    void shouldReturnCorrectEvent() {
        Event event = trigger.event();

        assertThat(event.getId()).isEqualTo("overdueResponse");
    }
}
