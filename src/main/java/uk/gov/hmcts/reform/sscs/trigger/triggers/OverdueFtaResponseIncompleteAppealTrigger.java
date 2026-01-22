package uk.gov.hmcts.reform.sscs.trigger.triggers;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.trigger.NightlyRunner;
import uk.gov.hmcts.reform.sscs.utility.calendar.BusinessDaysCalculatorService;

import java.time.LocalDate;

@Component
public class OverdueFtaResponseIncompleteAppealTrigger extends OverdueResponseTrigger {

    private static final String DATE_FIELD = "data.ftaCommunications.value.requestDateTime";

    private static final String CASE_STATE = "incompleteApplication";

    private static final String EVENT_NAME = "overdueFtaResponse";

    private static final Integer RESPONSE_DELAY = 2;

    private final BusinessDaysCalculatorService businessDaysCalculatorService;

    private final NightlyRunner nightlyRunner;


    public OverdueFtaResponseIncompleteAppealTrigger(LocalDate triggerDate,
                                                     BusinessDaysCalculatorService businessDaysCalculatorService,
                                                     NightlyRunner nightlyRunner) {
        super(triggerDate, DATE_FIELD, triggerDate, CASE_STATE, RESPONSE_DELAY, EVENT_NAME,
              businessDaysCalculatorService, nightlyRunner);
        this.businessDaysCalculatorService = businessDaysCalculatorService;
        this.nightlyRunner = nightlyRunner;
    }
}
