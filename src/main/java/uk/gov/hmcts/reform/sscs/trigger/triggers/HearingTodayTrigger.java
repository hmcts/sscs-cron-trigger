package uk.gov.hmcts.reform.sscs.trigger.triggers;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class HearingTodayTrigger extends DateTrigger {

    private static final String DATE_FIELD = "data.hearings.value.hearingDate";

    private static final String EVENT_NAME = "hearingToday";

    public HearingTodayTrigger(LocalDate triggerDate) {
        super(triggerDate, DATE_FIELD, triggerDate, EVENT_NAME);
    }
}
