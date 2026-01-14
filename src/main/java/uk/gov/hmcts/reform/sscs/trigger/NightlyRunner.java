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

    private final AuthorisationService authService;
    private final CoreCaseDataApi ccdApi;
    private final CaseEventsApi caseEventsApi;


    public NightlyRunner(AuthorisationService authService,
                         CoreCaseDataApi ccdApi, CaseEventsApi caseEventsApi) {
        this.ccdApi = ccdApi;
        this.caseEventsApi = caseEventsApi;
        this.authService = authService;
    }

    public List<CaseDetails> getCases(String query) {
        String accessToken = authService.getSystemUserAccessToken();
        log.debug(query);
        try {
            var searchResults = ccdApi.searchCases(accessToken, authService.getServiceToken(), CASE_TYPE, query);
            log.info("Matching cases found {}", searchResults.getTotal());
            return searchResults.getCases();
        } catch (Exception ex) {
            log.error("Failed to get cases for trigger: {}", getClass().getName());
            log.catching(ex);
            return List.of();
        }
    }

    public void submitEvents(String caseId, String eventId) {
        log.info("Checking case {}", caseId);

        String accessToken = authService.getSystemUserAccessToken();
        String serviceToken = authService.getServiceToken();
        String userId = authService.getSystemUserId();

        try {
            log.info("Triggering event {} on case {}", eventId, caseId);

            StartEventResponse startEventResponse = ccdApi
                .startEventForCaseWorker(accessToken, serviceToken, userId,
                                         JURISDICTION_ID, CASE_TYPE, caseId, eventId);

            CaseDataContent eventContent = CaseDataContent.builder()
                .eventToken(startEventResponse.getToken())
                .event(Event.builder().id(startEventResponse.getEventId()).build())
                .caseReference(caseId)
                .build();

            ccdApi.submitEventForCaseWorker(
                accessToken, serviceToken, userId, JURISDICTION_ID, CASE_TYPE, caseId, true, eventContent);
        } catch (Exception ex) {
            log.error("Failed to submit events for case {}", caseId);
            log.catching(ex);
        }
    }

    public List<CaseEventDetail> getEvents(String caseId) {
        try {
            String accessToken = authService.getSystemUserAccessToken();
            String serviceToken = authService.getServiceToken();
            String userId = authService.getSystemUserId();

            return caseEventsApi
                .findEventDetailsForCase(accessToken, serviceToken, userId, JURISDICTION_ID, CASE_TYPE, caseId);
        } catch (Exception ex) {
            log.error("Failed to get events for case {}", caseId);
            log.catching(ex);
            return List.of();
        }
    }
}
