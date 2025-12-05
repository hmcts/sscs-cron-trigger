package uk.gov.hmcts.reform.sscs.trigger;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.CaseEventsApi;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.CaseEventDetail;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.idam.client.IdamApi;
import uk.gov.hmcts.reform.sscs.trigger.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.trigger.triggers.Trigger;

import java.util.List;

import static java.util.Objects.requireNonNull;

@Component
@Log4j2
@SpringBootApplication
@ComponentScan(basePackages = {
    "uk.gov.hmcts.reform.idam.client",
    "uk.gov.hmcts.reform.ccd.client",
    "uk.gov.hmcts.reform.sscs"})
@EnableFeignClients(clients = { IdamApi.class })
@SuppressWarnings("PMD.DoNotTerminateVM")
public class NightlyRunner implements CommandLineRunner {

    private static final String CASE_TYPE = "Benefit";
    private static final String JURISDICTION_ID = "SSCS";

    private final AuthorisationService authorisationService;
    private final CoreCaseDataApi ccdApi;
    private final CaseEventsApi caseEventsApi;
    private final List<Trigger> triggers;


    public NightlyRunner(AuthorisationService authorisationService,
                         CoreCaseDataApi ccdApi, CaseEventsApi caseEventsApi,
                         List<Trigger> triggers) {
        requireNonNull(triggers, "triggers must not be null");
        this.ccdApi = ccdApi;
        this.caseEventsApi = caseEventsApi;
        this.triggers = triggers;
        this.authorisationService = authorisationService;
    }

    public static void main(final String[] args) {
        SpringApplication.run(NightlyRunner.class, args);
    }

    @Override
    public void run(String... args) {
        triggers.forEach(t -> execute(t));
    }

    private void execute(Trigger trigger) {
        log.info("Running trigger: {}", trigger.getClass().getName());
        String accessToken = authorisationService.getSystemUserAccessToken();
        String userId = authorisationService.getSystemUserId();

        String query = trigger.query();
        log.debug(query);

        do {
            SearchResult searchResults = checkForMatchingCases(accessToken, query, trigger);
            if (searchResults != null) {
                for (final CaseDetails caseDetails : searchResults.getCases()) {
                    try {
                        processCase(trigger, userId, caseDetails);
                        searchResults = checkForMatchingCases (accessToken, query, trigger);
                    } catch (Exception ex) {
                        log.error("Failed to process case {}", caseDetails.getId());
                        log.catching(ex);
                        break;
                    }
                }
            }
        } while (trigger.canRunMultipleTimes() && checkForMatchingCases(accessToken, query, trigger)!=null);
    }

    private void processCase(Trigger trigger, String userId, CaseDetails caseDetails) {
        String accessToken = authorisationService.getSystemUserAccessToken();
        final String caseId = Long.toString(caseDetails.getId());

        log.info("Checking case {}", caseId);

        final List<CaseEventDetail> events = caseEventsApi.findEventDetailsForCase(
            accessToken, authorisationService.getServiceToken(), userId,
            JURISDICTION_ID, CASE_TYPE, caseId);

        if (trigger.isValid(events)) {
            Event event = trigger.event();

            log.info("Triggering event {} on case {}", event.getId(), caseId);

            StartEventResponse startEventResponse = ccdApi.startEventForCaseWorker(
                accessToken, authorisationService.getServiceToken(), userId,
                JURISDICTION_ID, CASE_TYPE, caseId, event.getId());

            CaseDataContent eventContent = CaseDataContent.builder()
                .eventToken(startEventResponse.getToken())
                .event(Event.builder().id(startEventResponse.getEventId()).build())
                .caseReference(caseId)
                .build();

            ccdApi.submitEventForCaseWorker(accessToken, authorisationService.getServiceToken(), userId,
                                            JURISDICTION_ID, CASE_TYPE, caseId, true, eventContent);
        }
    }

    private SearchResult checkForMatchingCases(String accessToken, String query, Trigger trigger) {
        try {
            SearchResult searchResults = ccdApi.searchCases(accessToken,
                                                            authorisationService.getServiceToken(),CASE_TYPE, query);
            log.info("Matching cases found {}", searchResults.getTotal());
            return searchResults;
        } catch (Exception ex) {
            log.error("Failed to get cases for trigger: {}", trigger.getClass().getName());
            log.catching(ex);
            return null;
        }
    }
}
