package uk.gov.hmcts.reform.sscs.trigger;

import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.CaseEventsApi;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.CaseEventDetail;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.trigger.service.AuthorisationService;

@Component
@Log4j2
public class NightlyRunner {

    private static final String CASE_TYPE = "Benefit";
    private static final String JURISDICTION_ID = "SSCS";

    private final CoreCaseDataApi ccdApi;
    private final CaseEventsApi caseEventsApi;

    private final String userToken;
    private final String serviceToken;
    private final String userId;


    public NightlyRunner(AuthorisationService authService, CoreCaseDataApi ccdApi, CaseEventsApi caseEventsApi) {
        this.ccdApi = ccdApi;
        this.caseEventsApi = caseEventsApi;
        userToken = authService.getSystemUserAccessToken();
        serviceToken = authService.getServiceToken();
        userId = authService.getSystemUserId();
    }

    public List<CaseDetails> findCases(String query) {
        log.debug(query);
        try {
            var searchResults = ccdApi.searchCases(userToken, serviceToken, CASE_TYPE, query);
            log.info("Matching cases found {}", searchResults.getTotal());
            return searchResults.getCases();
        } catch (Exception ex) {
            log.error("Failed to get cases for trigger: {}", getClass().getName());
            log.catching(ex);
            return List.of();
        }
    }

    public List<CaseEventDetail> getCaseEvents(String caseId) {
        try {
            return caseEventsApi
                .findEventDetailsForCase(userToken, serviceToken, userId, JURISDICTION_ID, CASE_TYPE, caseId);
        } catch (Exception ex) {
            log.error("Failed to get events for case {}", caseId);
            log.catching(ex);
            return List.of();
        }
    }

    public void processCase(String caseId, String eventId) {
        log.info("Checking case {}", caseId);

        try {
            log.info("Triggering event {} on case {}", eventId, caseId);

            StartEventResponse startEventResponse = ccdApi
                .startEventForCaseWorker(userToken, serviceToken, userId, JURISDICTION_ID, CASE_TYPE, caseId, eventId);

            CaseDataContent eventContent = CaseDataContent.builder()
                .eventToken(startEventResponse.getToken())
                .event(Event.builder().id(startEventResponse.getEventId()).build())
                .caseReference(caseId)
                .build();

            ccdApi.submitEventForCaseWorker(
                userToken, serviceToken, userId, JURISDICTION_ID, CASE_TYPE, caseId, true, eventContent);
        } catch (Exception ex) {
            log.error("Failed to submit events for case {}", caseId);
            log.catching(ex);
        }
    }
}
