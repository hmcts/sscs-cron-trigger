package uk.gov.hmcts.reform.sscs.trigger.triggers;

import org.json.JSONArray;
import org.json.JSONObject;
import uk.gov.hmcts.reform.ccd.client.model.CaseEventDetail;
import uk.gov.hmcts.reform.ccd.client.model.Event;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class OverdueResponseTrigger implements Trigger {

    protected static final String DATE_FORMAT = "yyyy-MM-dd";
    protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);
    private final LocalDate triggerDate;

    private final String eventName;

    private final String dateField;

    private final String caseState;

    private final LocalDate queryDate;

    public OverdueResponseTrigger(LocalDate triggerDate, String dateField,
                                  LocalDate queryDate, String caseState, String eventName) {
        this.triggerDate = triggerDate;
        this.dateField = dateField;
        this.queryDate = queryDate;
        this.caseState = caseState;
        this.eventName = eventName;

    }

    @Override
    public String query() {
        return new JSONObject()
            .put("query", new JSONObject()
                .put("bool", new JSONObject()
                    .put("must", new JSONArray()
                        .put(new JSONObject()
                                 .put("match", new JSONObject()
                                     .put(dateField, queryDate.format(DATE_FORMATTER))))
                        .put(new JSONObject()
                                 .put("match", new JSONObject()
                                     .put("state", caseState))))
                    .put("must_not", new JSONArray()
                        .put(new JSONObject()
                                 .put("exists", new JSONObject()
                                     .put("field","data.ftaCommunications.value.requestReply"))))))
            .put("fields", new JSONArray()
                .put("reference"))
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
    public Event event() {
        return Event.builder()
            .id(eventName)
            .build();
    }
}
