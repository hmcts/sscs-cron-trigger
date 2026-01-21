package uk.gov.hmcts.reform.sscs.trigger.triggers;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseEventDetail;
import uk.gov.hmcts.reform.sscs.trigger.NightlyRunner;
import uk.gov.hmcts.reform.sscs.utility.calendar.BusinessDaysCalculatorService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OverdueFtaResponseIncompleteAppealTriggerTest {

    private Trigger trigger;
    private LocalDate testDate = LocalDate.of(2026, 1, 9);

    @Mock
    private BusinessDaysCalculatorService businessDaysCalculatorService;

    @Mock
    private NightlyRunner nightlyRunner;

    @BeforeEach
    void setup() throws IOException {
        trigger = new OverdueFtaResponseIncompleteAppealTrigger(testDate, businessDaysCalculatorService, nightlyRunner);
    }

    @Test
    void shouldReturnValidQuery() throws IOException {
        final String dateFormat = "yyyy-MM-dd";
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateFormat);
        LocalDate overdueDate = LocalDate.of(2026, 1, 7);

        given(businessDaysCalculatorService.getBusinessDayInPast(testDate, 2))
            .willReturn(overdueDate);

        String query = trigger.query();

        JSONObject result = new JSONObject(query);
        assertThat(result.query("/fields/0"))
            .isEqualTo("reference");
        assertThat(result.query("/_source"))
            .isEqualTo(false);
        assertThat(result.query("/query/bool/must/0/range/data.ftaCommunications.value.requestDateTime/lte"))
            .isEqualTo(overdueDate.atStartOfDay().toString());
        assertThat(result.query("/query/bool/must/1/match/state"))
            .isEqualTo("incompleteApplication");
        assertThat(result.query("/query/bool/must_not/0/exists/field"))
            .isEqualTo("data.ftaCommunications.value.requestReply");
        assertThat(result.query("/query/bool/must_not/1/match/data.ftaCommunications.value.taskCreatedForRequest"))
            .isEqualTo("Yes");
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
        String event = trigger.event();

        assertThat(event).isEqualTo("overdueFtaResponse");
    }
}
