package uk.gov.hmcts.reform.sscs.trigger.triggers;

import org.json.JSONArray;
import org.json.JSONObject;
import uk.gov.hmcts.reform.ccd.client.model.CaseEventDetail;
import uk.gov.hmcts.reform.ccd.client.model.Event;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DateTrigger implements Trigger {

    protected static final String DATE_FORMAT = "yyyy-MM-dd";
    protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);
    private final  LocalDate triggerDate;

    private final String eventName;

    private final String dateField;

    private final LocalDate queryDate;

    public DateTrigger(LocalDate triggerDate, String dateField, LocalDate queryDate, String eventName) {
        this.triggerDate = triggerDate;
        this.dateField = dateField;
        this.queryDate = queryDate;
        this.eventName = eventName;
    }

    @Override
    public String query() {
        return new JSONObject()
            .put("query", new JSONObject()
                .put("bool", new JSONObject()
                    .put("filter", new JSONArray()
                        .put(new JSONObject().put("range", new JSONObject()
                            .put(dateField, new JSONObject()
                                .put("gte", queryDate.format(DATE_FORMATTER))
                                .put("lte", queryDate.plusDays(1).format(DATE_FORMATTER))
                                .put("format",DATE_FORMAT)))))))
            .put("fields", new JSONArray()
                .put("reference"))
            .put("_source", false)
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
