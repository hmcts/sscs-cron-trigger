package uk.gov.hmcts.reform.sscs.trigger.triggers;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class OverdueFtaResponseIncompleteAppealTrigger extends OverdueResponseTrigger {

    private static final String DATE_FIELD = "data.ftaCommunications.value.requestDateTime";

    private static final String CASE_STATE = "readyToList";

    private static final String EVENT_NAME = "overdueResponse";

    public OverdueFtaResponseIncompleteAppealTrigger(LocalDate triggerDate) {
        super(triggerDate, DATE_FIELD, triggerDate, CASE_STATE, EVENT_NAME);
    }
}
