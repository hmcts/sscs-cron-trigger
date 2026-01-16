package uk.gov.hmcts.reform.sscs.trigger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.CaseEventsApi;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.CaseEventDetail;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.trigger.service.AuthorisationService;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NightlyRunnerTest {

    private static final String EVENT_TOKEN = "EVENT-TOKEN";
    private static final String ACCESS_TOKEN = "ACCESS-TOKEN";
    private static final String SERVICE_TOKEN = "SERVICE-TOKEN";
    private static final String USER_ID = "USER-ID";
    private static final String CASE_TYPE = "Benefit";
    private static final String JURISDICTION_ID = "SSCS";
    private static final String TEST_EVENT_ID = "testEvent";
    private static final String TEST_QUERY = "{\"query\":{\"match\":{\"field\":\"value\"}}}";
    private static final String CASE_ID = "1111";

    @Mock
    private AuthorisationService authService;
    @Mock
    private CoreCaseDataApi ccdApi;
    @Mock
    private CaseEventsApi caseEventsApi;

    private NightlyRunner nightlyRunner;

    @BeforeEach
    void setUp() {
        // Use lenient() to avoid strict stubbing issues
        lenient().when(authService.getSystemUserAccessToken()).thenReturn(ACCESS_TOKEN);
        lenient().when(authService.getSystemUserId()).thenReturn(USER_ID);
        lenient().when(authService.getServiceToken()).thenReturn(SERVICE_TOKEN);

        // Create NightlyRunner after setting up the mocks
        nightlyRunner = new NightlyRunner(authService, ccdApi, caseEventsApi);
    }

    @Test
    void findCases_shouldReturnCasesList() {
        // Given
        List<CaseDetails> caseList = List.of(CaseDetails.builder().id(1L).build());
        when(ccdApi.searchCases(ACCESS_TOKEN, SERVICE_TOKEN, CASE_TYPE, TEST_QUERY))
            .thenReturn(SearchResult.builder().cases(caseList).total(1).build());

        // When
        List<CaseDetails> result = nightlyRunner.findCases(TEST_QUERY);

        // Then
        assertEquals(caseList, result);
    }

    @Test
    void findCases_shouldReturnEmptyListWhenExceptionOccurs() {
        // Given
        when(ccdApi.searchCases(ACCESS_TOKEN, SERVICE_TOKEN, CASE_TYPE, TEST_QUERY))
            .thenThrow(new RuntimeException("Test exception"));

        // When
        List<CaseDetails> result = nightlyRunner.findCases(TEST_QUERY);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getCaseEvents_shouldReturnEventsList() {
        // Given
        var caseEvents = List.of(CaseEventDetail.builder().id("someEvent").createdDate(LocalDateTime.now()).build());
        when(caseEventsApi
                 .findEventDetailsForCase(ACCESS_TOKEN, SERVICE_TOKEN, USER_ID, JURISDICTION_ID, CASE_TYPE, CASE_ID))
            .thenReturn(caseEvents);

        // When
        List<CaseEventDetail> result = nightlyRunner.getCaseEvents(CASE_ID);

        // Then
        assertEquals(caseEvents, result);
    }

    @Test
    void getCaseEvents_shouldReturnEmptyListWhenExceptionOccurs() {
        // Given
        when(caseEventsApi
                 .findEventDetailsForCase(ACCESS_TOKEN, SERVICE_TOKEN, USER_ID, JURISDICTION_ID, CASE_TYPE, CASE_ID))
            .thenThrow(new RuntimeException("Test exception"));

        // When
        List<CaseEventDetail> result = nightlyRunner.getCaseEvents(CASE_ID);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void processCase_shouldStartAndSubmitEvent() {
        // Given
        StartEventResponse startEventResponse =
            StartEventResponse.builder().token(EVENT_TOKEN).eventId(TEST_EVENT_ID).build();

        when(ccdApi.startEventForCaseWorker(
            ACCESS_TOKEN, SERVICE_TOKEN, USER_ID, JURISDICTION_ID, CASE_TYPE, CASE_ID, TEST_EVENT_ID))
            .thenReturn(startEventResponse);

        // When
        nightlyRunner.processCase(CASE_ID, TEST_EVENT_ID);

        // Then
        verify(ccdApi, times(1)).startEventForCaseWorker(
            ACCESS_TOKEN, SERVICE_TOKEN, USER_ID, JURISDICTION_ID, CASE_TYPE, CASE_ID, TEST_EVENT_ID);

        verify(ccdApi, times(1)).submitEventForCaseWorker(
            eq(ACCESS_TOKEN), eq(SERVICE_TOKEN), eq(USER_ID), eq(JURISDICTION_ID),
            eq(CASE_TYPE), eq(CASE_ID), eq(true), any(CaseDataContent.class));
    }

    @Test
    void processCase_shouldHandleExceptionGracefully() {
        // Given
        doThrow(new RuntimeException("Test exception"))
            .when(ccdApi).startEventForCaseWorker(
                ACCESS_TOKEN, SERVICE_TOKEN, USER_ID, JURISDICTION_ID, CASE_TYPE, CASE_ID, TEST_EVENT_ID);

        // When/Then
        assertDoesNotThrow(() -> nightlyRunner.processCase(CASE_ID, TEST_EVENT_ID));

        verify(ccdApi, times(1)).startEventForCaseWorker(
            ACCESS_TOKEN, SERVICE_TOKEN, USER_ID, JURISDICTION_ID, CASE_TYPE, CASE_ID, TEST_EVENT_ID);
        verify(ccdApi, times(0)).submitEventForCaseWorker(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean(), any());
    }
}
