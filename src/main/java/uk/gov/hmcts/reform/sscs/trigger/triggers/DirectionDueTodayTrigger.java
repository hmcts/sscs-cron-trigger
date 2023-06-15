package uk.gov.hmcts.reform.sscs.trigger.triggers;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DirectionDueTodayTrigger extends DateTrigger {

    private static final String DATE_FIELD = "data.directionDueDate";

    private static final String EVENT_NAME = "directionDueToday";

    public DirectionDueTodayTrigger(LocalDate triggerDate) {
        super(triggerDate, DATE_FIELD, triggerDate, EVENT_NAME);
    }
}
