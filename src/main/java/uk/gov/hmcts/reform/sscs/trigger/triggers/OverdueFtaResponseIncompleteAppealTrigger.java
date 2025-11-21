package uk.gov.hmcts.reform.sscs.trigger.triggers;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class OverdueFtaResponseIncompleteAppealTrigger extends DateTrigger {

    private static final String DATE_FIELD = "data.ftaCommunications[0].value.requestDateTime";

    private static final String EVENT_NAME = "overdueResponse";

    public OverdueFtaResponseIncompleteAppealTrigger(LocalDate triggerDate) {
        super(triggerDate, DATE_FIELD, triggerDate, EVENT_NAME);
    }
}
