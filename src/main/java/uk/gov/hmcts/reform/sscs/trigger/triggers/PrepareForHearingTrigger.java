package uk.gov.hmcts.reform.sscs.trigger.triggers;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import uk.gov.hmcts.reform.sscs.trigger.NightlyRunner;

@Component
public class PrepareForHearingTrigger extends DateTrigger {

    private static final String DATE_FIELD = "data.hearings.value.hearingDate";

    private static final String EVENT_NAME = "prepareForHearing";


    public PrepareForHearingTrigger(NightlyRunner nightlyRunner, LocalDate triggerDate) {
        super(nightlyRunner, triggerDate,DATE_FIELD, triggerDate.plusDays(14), EVENT_NAME);
    }
}
