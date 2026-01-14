package uk.gov.hmcts.reform.sscs.trigger.triggers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.json.JSONArray;
import org.json.JSONObject;
import uk.gov.hmcts.reform.ccd.client.model.CaseEventDetail;
import uk.gov.hmcts.reform.sscs.trigger.NightlyRunner;

@Log4j2
public class DateTrigger implements Trigger {

    protected static final String DATE_FORMAT = "yyyy-MM-dd";
    protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);

    private final NightlyRunner nightlyRunner;

    private final LocalDate triggerDate;
    private final String eventName;
    private final String dateField;
    private final LocalDate queryDate;

    public DateTrigger(NightlyRunner nightlyRunner,
                       LocalDate triggerDate, String dateField, LocalDate queryDate, String eventName) {
        this.nightlyRunner = nightlyRunner;
        this.triggerDate = triggerDate;
        this.dateField = dateField;
        this.queryDate = queryDate;
        this.eventName = eventName;
    }

    @Override
    public void execute() {
        log.info("Running trigger: {}", getClass().getName());
        nightlyRunner.getCases(query())
            .forEach(caseDetails -> processCase(caseDetails.getId().toString()));
    }

    @Override
    public void processCase(String caseId) {
        log.info("Processing case {}", caseId);
        if (isValid(nightlyRunner.getEvents(caseId))) {
            nightlyRunner.submitEvents(caseId, event());
        }
    }

    @Override
    public String query() {
        return new JSONObject()
            .put("query", new JSONObject()
                .put("match", new JSONObject()
                    .put(dateField, queryDate.format(DATE_FORMATTER))
                )
            )
            .put("fields", new JSONArray().put("reference"))
            .put("_source", false)
            .put("size", 10_000)
            .toString();
    }

    @Override
    public boolean isValid(List<CaseEventDetail> events) {
        return events.stream()
            .filter(e -> eventName.equals(e.getId()))
            .noneMatch(e -> e.getCreatedDate().isAfter(triggerDate.atStartOfDay()));
    }

    @Override
    public String event() {
        return eventName;
    }
}
