package uk.gov.hmcts.reform.sscs.trigger.triggers;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class PrepareForHearingTrigger extends DateTrigger {

    private static final String DATE_FIELD = "data.hearings.value.hearingDate";

    private static final String EVENT_NAME = "prepareForHearing";


    public PrepareForHearingTrigger(LocalDate triggerDate) {
        super(triggerDate,DATE_FIELD, triggerDate.plusDays(14), EVENT_NAME);
    }
}
