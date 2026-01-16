package uk.gov.hmcts.reform.sscs.trigger.triggers;

import org.json.JSONArray;
import org.json.JSONObject;
import uk.gov.hmcts.reform.ccd.client.model.CaseEventDetail;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.sscs.service.BusinessDaysCalculatorService;

import java.io.IOException;
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

    private final Integer responseDelay;

    private final LocalDate queryDate;

    private final static Integer querySize = 10_000;

    private final BusinessDaysCalculatorService businessDaysCalculatorService;

    public OverdueResponseTrigger(LocalDate triggerDate, String dateField,
                                  LocalDate queryDate, String caseState, Integer responseDelay, String eventName,
                                  BusinessDaysCalculatorService businessDaysCalculatorService) {
        this.triggerDate = triggerDate;
        this.dateField = dateField;
        this.queryDate = queryDate;
        this.caseState = caseState;
        this.responseDelay = responseDelay;
        this.eventName = eventName;
        this.businessDaysCalculatorService = businessDaysCalculatorService;
    }

    @Override
    public String query() {
        return new JSONObject()
            .put("query", new JSONObject()
                .put("bool", new JSONObject()
                    .put("must", new JSONArray()
                        .put(new JSONObject()
                                 .put("match", new JSONObject()
                                     .put(dateField, getRequestDate(queryDate, responseDelay))))
                        .put(new JSONObject()
                                 .put("match", new JSONObject()
                                     .put("state", caseState))))
                    .put("must_not", new JSONArray()
                        .put(new JSONObject()
                                 .put("exists", new JSONObject()
                                     .put("field","data.ftaCommunications.value.requestReply")))
                        .put(new JSONObject()
                                 .put("match", new JSONObject()
                                     .put("data.ftaCommunications.value.taskCreatedForRequest","Yes"))))))
            .put("fields", new JSONArray()
                .put("reference"))
            .put("_source", false)
            .put("size", querySize)
            .toString();
    }

    @Override
    public boolean isValid(List<CaseEventDetail> events) {
        return true;
    }

    @Override
    public Event event() {
        return Event.builder()
            .id(eventName)
            .build();
    }

    private String getRequestDate(LocalDate queryDate, Integer responseDelay) {
        try {
            return businessDaysCalculatorService.getBusinessDayInPast(queryDate, responseDelay).toString();
        } catch (IOException e) {
            return queryDate.minusDays(responseDelay).format(DATE_FORMATTER);
        }
    }
}
