package uk.gov.hmcts.reform.sscs.trigger.triggers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequestTopic;
import uk.gov.hmcts.reform.sscs.trigger.NightlyRunner;
import uk.gov.hmcts.reform.sscs.utility.calendar.BusinessDaysCalculatorService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class OverdueResponseTriggerTest {

    private LocalDate triggerDate;
    private String dateField;
    private LocalDate queryDate;
    private String caseState;
    private Integer responseDelay;
    private String eventName;
    private OverdueResponseTrigger overdueResponseTrigger;

    @Mock
    private NightlyRunner nightlyRunner;

    @Mock
    private BusinessDaysCalculatorService businessDaysCalculatorService;


    @BeforeEach
    void setup() throws IOException {
        overdueResponseTrigger = new OverdueResponseTrigger(triggerDate, dateField, queryDate, caseState, responseDelay,
                                                            eventName, businessDaysCalculatorService, nightlyRunner);
    }

    @Test
    public void shouldConvertMapToSscsCaseData() {
        var ftaCommunication =
            new HashMap<String, Object>(Map.of("id", "12345",
                                               "value", Map.of(
                                                   "requestTopic", "issuingOffice",
                                                   "requestDateTime", "2024-01-01T10:00:00",
                                                   "requestMessage", "test",
                                                   "taskCreatedForRequest", "No")));

        var caseData = new HashMap<String, Object>(Map.of("processingVenue", "Bradford",
                                                          "ftaCommunications", List.of(ftaCommunication)));
        var sscsCaseData = OverdueResponseTrigger.convertToSscsCaseData(caseData);
        CommunicationRequest ftaRequest = sscsCaseData.getCommunicationFields().getFtaCommunications().getFirst();

        assertEquals("Bradford", sscsCaseData.getProcessingVenue());
        assertThat("12345").isEqualTo(ftaRequest.getId());
        assertThat(CommunicationRequestTopic.ISSUING_OFFICE).isEqualTo(ftaRequest.getValue().getRequestTopic());
        assertThat(LocalDateTime.of(2024, 1, 1, 10, 0))
            .isEqualTo(ftaRequest.getValue().getRequestDateTime());
        assertThat("No").isEqualTo(ftaRequest.getValue().getTaskCreatedForRequest());
    }
}
