package uk.gov.hmcts.reform.sscs.trigger.triggers;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import uk.gov.hmcts.reform.sscs.trigger.NightlyRunner;

@Component
public class HearingTodayTrigger extends DateTrigger {

    private static final String DATE_FIELD = "data.hearings.value.hearingDate";

    private static final String EVENT_NAME = "hearingToday";

    public HearingTodayTrigger(NightlyRunner nightlyRunner, LocalDate triggerDate) {
        super(nightlyRunner, triggerDate, DATE_FIELD, triggerDate, EVENT_NAME);
    }
}
