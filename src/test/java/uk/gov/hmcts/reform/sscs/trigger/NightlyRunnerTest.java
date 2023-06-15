package uk.gov.hmcts.reform.sscs.trigger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import uk.gov.hmcts.reform.ccd.client.CaseEventsApi;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.CaseEventDetail;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.trigger.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.trigger.triggers.DateTrigger;
import uk.gov.hmcts.reform.sscs.trigger.triggers.Trigger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NightlyRunnerTest {

    private static final String EVENT_TOKEN = "EVENT-TOKEN";
    private static final String ACCESS_TOKEN = "ACCESS-TOKEN";
    private static final String SERVICE_TOKEN = "SERVICE-TOKEN";
    private static final String USER_ID = "USER-ID";
    private static final String CASE_TYPE = "Benefit";
    private static final String JURISDICTION_ID = "SSCS";
    private static final String TEST_EVENT_ID = "testEvent";
    private static final String TEST_CASE_DATE_PROPERTY = "testDate";

    private static final long CASE_1_ID = 1111111111111111L;

    private static final CaseDetails CASE_1_DETAILS = CaseDetails.builder().id(CASE_1_ID).build();

    private static final CaseEventDetail SOME_EVENT = CaseEventDetail.builder()
        .id("someEvent")
        .createdDate(LocalDateTime.now()).build();

    private static final CaseEventDetail HEARING_TODAY_EVENT = CaseEventDetail.builder()
        .id(TEST_EVENT_ID)
        .createdDate(LocalDateTime.now()).build();


    @Mock
    private AuthorisationService authorisationService;
    @Spy
    private CoreCaseDataApi ccdApi;
    @Spy
    private CaseEventsApi caseEventsApi;
    @Spy
    private List<Trigger> triggers = new ArrayList<>();

    @InjectMocks
    private NightlyRunner runner;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);

        triggers.add(new DateTrigger(LocalDate.now(), TEST_CASE_DATE_PROPERTY, LocalDate.now(), TEST_EVENT_ID));

        when(authorisationService.getSystemUserAccessToken()).thenReturn(ACCESS_TOKEN);
        when(authorisationService.getSystemUserId()).thenReturn(USER_ID);
        when(authorisationService.getServiceToken()).thenReturn(SERVICE_TOKEN);

        doReturn(SearchResult.builder().cases(Arrays.asList(CASE_1_DETAILS)).build())
            .when(ccdApi).searchCases(any(), any(), any(), anyString());

        doReturn(StartEventResponse.builder().token(EVENT_TOKEN).eventId(TEST_EVENT_ID).build())
            .when(ccdApi).startEventForCaseWorker(any(), any(), any(), any(), any(), any(), anyString());

        doReturn(CaseDetails.builder().build())
            .when(ccdApi).submitEventForCaseWorker(any(), any(), any(), any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void hearingToday_shouldTriggerHearingTodayEvent() {
        /* Setup */
        doReturn(Arrays.asList(SOME_EVENT))
            .when(caseEventsApi).findEventDetailsForCase(
                ACCESS_TOKEN, SERVICE_TOKEN, USER_ID,
                JURISDICTION_ID, CASE_TYPE, Long.toString(CASE_1_ID));

        /* Run */
        runner.run();

        /* Verify */
        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        verify(ccdApi, times(1)).searchCases(any(), any(), any(), query.capture());
        assertThat(query.getValue()).contains(TEST_CASE_DATE_PROPERTY);

        verify(caseEventsApi, times(1)).findEventDetailsForCase(
            eq(ACCESS_TOKEN), eq(SERVICE_TOKEN), eq(USER_ID), eq(JURISDICTION_ID),
            eq(CASE_TYPE), eq(Long.toString(CASE_1_ID)));

        verify(ccdApi, times(1)).startEventForCaseWorker(
            eq(ACCESS_TOKEN), eq(SERVICE_TOKEN), eq(USER_ID), eq(JURISDICTION_ID),
            eq(CASE_TYPE), eq(Long.toString(CASE_1_ID)), eq(TEST_EVENT_ID));

        ArgumentCaptor<CaseDataContent> body = ArgumentCaptor.forClass(CaseDataContent.class);
        verify(ccdApi, times(1)).submitEventForCaseWorker(
            eq(ACCESS_TOKEN), eq(SERVICE_TOKEN), eq(USER_ID), eq(JURISDICTION_ID),
            eq(CASE_TYPE), eq(Long.toString(CASE_1_ID)), eq(true), body.capture());
        assertThat(body.getValue().getEvent().getId()).isEqualTo(TEST_EVENT_ID);
        assertThat(body.getValue().getEventToken()).isEqualTo(EVENT_TOKEN);
    }

    @Test
    void hearingTodayEventAlreadyTriggered_shouldNotTriggerEvent() {
        /* Setup */
        doReturn(Arrays.asList(SOME_EVENT, HEARING_TODAY_EVENT))
            .when(caseEventsApi).findEventDetailsForCase(
                ACCESS_TOKEN, SERVICE_TOKEN, USER_ID,
                JURISDICTION_ID, CASE_TYPE, Long.toString(CASE_1_ID));

        /* Run */
        runner.run();

        /* Verify */
        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        verify(ccdApi, times(1)).searchCases(any(), any(), any(), query.capture());
        assertThat(query.getValue()).contains(TEST_CASE_DATE_PROPERTY);

        verify(caseEventsApi, times(1)).findEventDetailsForCase(
            eq(ACCESS_TOKEN), eq(SERVICE_TOKEN), eq(USER_ID), eq(JURISDICTION_ID),
            eq(CASE_TYPE), eq(Long.toString(CASE_1_ID)));

        verify(ccdApi, times(0)).startEventForCaseWorker(
            anyString(), anyString(), anyString(), anyString(),
            anyString(), anyString(), anyString());

        verify(ccdApi, times(0)).submitEventForCaseWorker(
            anyString(), anyString(), anyString(), anyString(),
            anyString(), anyString(), anyBoolean(), any());
    }

    @Test
    void ccdException_shouldThrowException() {
        when(ccdApi.searchCases(eq(ACCESS_TOKEN), eq(SERVICE_TOKEN), eq(CASE_TYPE), any()))
            .thenThrow(new RuntimeException("CCD Exception"));

        assertThrows(
            RuntimeException.class,
            () -> runner.run(),
            "Exceptions should be thrown"
        );
    }
}
