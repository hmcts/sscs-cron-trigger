package uk.gov.hmcts.reform.sscs.trigger.triggers;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.trigger.NightlyRunner;

import java.time.LocalDate;

@Component
public class PrepareForHearingTrigger extends DateTrigger {

    private static final String DATE_FIELD = "data.hearings.value.hearingDate";
    private static final String EVENT_NAME = "prepareForHearing";
    public static final int DAYS_TO_HEARING = 14;


    public PrepareForHearingTrigger(NightlyRunner nightlyRunner, LocalDate triggerDate) {
        super(nightlyRunner, triggerDate, DATE_FIELD, triggerDate.plusDays(DAYS_TO_HEARING), EVENT_NAME);
    }
}
