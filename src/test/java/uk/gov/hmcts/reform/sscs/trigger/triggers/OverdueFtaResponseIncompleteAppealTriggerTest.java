package uk.gov.hmcts.reform.sscs.trigger.triggers;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.CaseEventDetail;
import uk.gov.hmcts.reform.sscs.trigger.NightlyRunner;
import uk.gov.hmcts.reform.sscs.utility.calendar.BusinessDaysCalculatorService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OverdueFtaResponseIncompleteAppealTriggerTest {

    private Trigger trigger;
    private final LocalDate testDate = LocalDate.of(2026, 1, 9);
    private final LocalDate overdueDate = LocalDate.of(2026, 1, 7);

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

        given(businessDaysCalculatorService.getBusinessDayInPast(testDate, 2))
            .willReturn(overdueDate);

        String query = trigger.query();

        JSONObject result = new JSONObject(query);
        assertThat(result.query("/fields/0"))
            .isEqualTo("reference");
        assertThat(result.query("/_source"))
            .isEqualTo(false);
        assertThat(result.query("/query/bool/must/0/range/data.ftaCommunications.value.requestDateTime/lte"))
            .isEqualTo(dateTimeFormatter.format(overdueDate));
        assertThat(result.query("/query/bool/must/1/match/state"))
            .isEqualTo("incompleteApplication");
        assertThat(result.query("/query/bool/must/2/match/data.ftaCommunications.value.taskCreatedForRequest"))
            .isEqualTo("No");
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

    @Test
    void givenOneOverdueCommunication_shouldProcessCaseOnce() throws IOException {

        LocalDate overdueDate = LocalDate.of(2026, 1, 7);

        given(businessDaysCalculatorService.getBusinessDayInPast(testDate, 2))
            .willReturn(overdueDate);

        var ftaCommunication =
            new HashMap<String, Object>(Map.of("id", "12345",
                                               "value", Map.of(
                    "requestTopic", "issuingOffice",
                    "requestMessage", "test",
                    "requestDateTime", "2026-01-07T10:00:00",
                    "taskCreatedForRequest", "No")));

        var caseData = new HashMap<String, Object>(Map.of("processingVenue", "Bradford",
                                                          "ftaCommunications", List.of(ftaCommunication)));

        CaseDetails caseDetails = CaseDetails.builder()
            .id(1L)
            .data(caseData).build();

        trigger.processCase(caseDetails);

        verify(nightlyRunner, times(1)).processCase("1", "overdueFtaResponse");
    }


    @Test
    void givenTwoOverdueCommunications_shouldProcessCaseTwice() throws IOException {

        LocalDate overdueDate = LocalDate.of(2026, 1, 7);

        given(businessDaysCalculatorService.getBusinessDayInPast(testDate, 2))
            .willReturn(overdueDate);

        var ftaCommunication1 =
            new HashMap<String, Object>(Map.of("id", "12345",
                                               "value", Map.of(
                    "requestTopic", "issuingOffice",
                    "requestMessage", "test",
                    "requestDateTime", "2026-01-07T10:00:00",
                    "taskCreatedForRequest", "No")));

        var ftaCommunication2 =
            new HashMap<String, Object>(Map.of("id", "54321",
                                               "value", Map.of(
                    "requestTopic", "mrnDetails",
                    "requestMessage", "test message",
                    "requestDateTime", "2026-01-05T10:00:00",
                    "taskCreatedForRequest", "No")));

        var ftaCommunication3 =
            new HashMap<String, Object>(Map.of("id", "54321",
                                               "value", Map.of(
                    "requestTopic", "mrnDetails",
                    "requestMessage", "test message",
                    "requestDateTime", "2026-01-10T10:00:00",
                    "taskCreatedForRequest", "No")));

        var caseData = new HashMap<String, Object>(Map.of("processingVenue", "Bradford",
                                                          "ftaCommunications", List.of(ftaCommunication1,
                                                                                       ftaCommunication2,
                                                                                       ftaCommunication3)));

        CaseDetails caseDetails = CaseDetails.builder()
            .id(1L)
            .data(caseData).build();

        trigger.processCase(caseDetails);

        verify(nightlyRunner, times(2)).processCase("1", "overdueFtaResponse");
    }

    @Test
    void givenNoOverdueCommunications_shouldNotProcessCase() throws IOException {

        LocalDate overdueDate = LocalDate.of(2026, 1, 7);

        given(businessDaysCalculatorService.getBusinessDayInPast(testDate, 2))
            .willReturn(overdueDate);

        var caseData = new HashMap<String, Object>(Map.of("processingVenue", "Bradford"));

        CaseDetails caseDetails = CaseDetails.builder()
            .id(1L)
            .data(caseData).build();

        trigger.processCase(caseDetails);

        verify(nightlyRunner, times(0)).processCase("1", "overdueFtaResponse");
    }

    @Test
    void givenCommunicationsWithReplyOrTaskCreated_shouldNotProcessCase() throws IOException {

        LocalDate overdueDate = LocalDate.of(2026, 1, 7);

        given(businessDaysCalculatorService.getBusinessDayInPast(testDate, 2))
            .willReturn(overdueDate);

        var ftaCommunication1 =
            new HashMap<String, Object>(Map.of("id", "12345",
                                               "value", Map.of(
                    "requestReply", Map.of(
                        "replyMessage", "This is a reply",
                        "replyDateTime", "2026-01-08T10:00:00"
                    ),
                    "requestTopic", "issuingOffice",
                    "requestMessage", "test",
                    "requestDateTime", "2026-01-07T10:00:00",
                    "taskCreatedForRequest", "No")));

        var ftaCommunication2 =
            new HashMap<String, Object>(Map.of("id", "54321",
                                               "value", Map.of(
                    "requestTopic", "mrnDetails",
                    "requestMessage", "test message",
                    "requestDateTime", "2026-01-05T10:00:00",
                    "taskCreatedForRequest", "Yes")));

        var caseData = new HashMap<String, Object>(Map.of("processingVenue", "Bradford",
                                                          "ftaCommunications", List.of(ftaCommunication1,
                                                                                       ftaCommunication2)));

        CaseDetails caseDetails = CaseDetails.builder()
            .id(1L)
            .data(caseData).build();

        trigger.processCase(caseDetails);

        verify(nightlyRunner, times(0)).processCase("1", "overdueFtaResponse");
    }
}
