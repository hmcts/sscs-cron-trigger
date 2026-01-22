package uk.gov.hmcts.reform.sscs.trigger.triggers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.log4j.Log4j2;
import org.json.JSONArray;
import org.json.JSONObject;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.CaseEventDetail;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.trigger.NightlyRunner;
import uk.gov.hmcts.reform.sscs.utility.calendar.BusinessDaysCalculatorService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Log4j2
public class OverdueResponseTrigger implements Trigger {

    protected static final String DATE_FORMAT = "yyyy-MM-dd";
    protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);
    private final LocalDate triggerDate;

    private final String eventName;

    private final String dateField;

    private final String caseState;

    private final Integer responseDelay;

    private final LocalDate queryDate;

    private static final Integer querySize = 10_000;

    private final BusinessDaysCalculatorService businessDaysCalculatorService;

    private final NightlyRunner nightlyRunner;

    public OverdueResponseTrigger(LocalDate triggerDate, String dateField,
                                  LocalDate queryDate, String caseState, Integer responseDelay, String eventName,
                                  BusinessDaysCalculatorService businessDaysCalculatorService,
                                  NightlyRunner nightlyRunner) {
        this.triggerDate = triggerDate;
        this.dateField = dateField;
        this.queryDate = queryDate;
        this.caseState = caseState;
        this.responseDelay = responseDelay;
        this.eventName = eventName;
        this.businessDaysCalculatorService = businessDaysCalculatorService;
        this.nightlyRunner = nightlyRunner;
    }

    @Override
    public void processCase(CaseDetails caseDetails) {
        String caseId = caseDetails.getId().toString();
        log.info("Processing case {}", caseId);
        if (isValid(nightlyRunner.getCaseEvents(caseId))) {

            var caseData = convertToSscsCaseData(caseDetails.getData());
            List<CommunicationRequest> ftaCommunications = caseData.getCommunicationFields().getFtaCommunications();

            LocalDate overdueDate = LocalDate.parse(getRequestDate(queryDate, responseDelay));

            List<CommunicationRequest> overdueCommunications = ftaCommunications.stream()
                .filter(request -> request.getValue().getRequestReply() == null
                    && request.getValue().getTaskCreatedForRequest() != YesNo.YES
                    && (request.getValue().getRequestDateTime().toLocalDate().isEqual(overdueDate)
                    || request.getValue().getRequestDateTime().toLocalDate().isBefore(overdueDate)))
                .toList();

            log.info(overdueCommunications);

            for (CommunicationRequest request : overdueCommunications) {
                nightlyRunner.processCase(caseId, event());

            }
        }
    }

    public String query2() {
        return new JSONObject()
            .put("query", new JSONObject()
                .put("bool", new JSONObject()
                    .put("must", new JSONArray()
                         .put(new JSONObject()
                                  .put("range", new JSONObject()
                                      .put(dateField, new JSONObject()
                                          .put("lte", getRequestDate(queryDate, responseDelay))))
                                  .put("must_not", new JSONArray()
                                      .put(new JSONObject()
                                               .put("exists", new JSONObject()
                                                   .put("field","data.ftaCommunications.value.requestReply")))
                                      .put(new JSONObject()
                                               .put("match", new JSONObject()
                                                   .put("data.ftaCommunications.value.taskCreatedForRequest","Yes")))))
                         .put(new JSONObject()
                                  .put("match", new JSONObject()
                                      .put("state", caseState)))
                    )
                )
            )
            .put("fields", new JSONArray()
                .put("reference"))
            .put("_source", false)
            .put("size", querySize)
            .toString();
    }

    @Override
    public String query() {
        return new JSONObject()
            .put("query", new JSONObject()
                .put("bool", new JSONObject()
                    .put("must", new JSONArray()
                        .put(new JSONObject()
                            .put("query", new JSONObject()
                                .put("bool", new JSONObject()
                                    .put("must", new JSONArray()
                                        .put(new JSONObject()
                                            .put("range", new JSONObject()
                                                .put(dateField, new JSONObject()
                                                    .put("lte", getRequestDate(queryDate, responseDelay))))))
                                    .put("must_not", new JSONArray()
                                        .put(new JSONObject()
                                            .put("exists", new JSONObject()
                                                .put("field", "data.ftaCommunications.value.requestReply")))
                                        .put(new JSONObject()
                                            .put("match", new JSONObject()
                                                .put("data.ftaCommunications.value.taskCreatedForRequest", "Yes"))))
                                )
                            )
                        )
                        .put(new JSONObject()
                                 .put("match", new JSONObject()
                                     .put("state", caseState)))
                    )
                )
            )
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
    public String event() {
        return eventName;
    }

    private String getRequestDate(LocalDate queryDate, Integer responseDelay) {
        try {
            return businessDaysCalculatorService.getBusinessDayInPast(queryDate, responseDelay).toString();
        } catch (IOException e) {
            return queryDate.minusDays(responseDelay).format(DATE_FORMATTER);
        }
    }

    protected SscsCaseData convertToSscsCaseData(Map<String, Object> caseData) {
        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper.convertValue(caseData, SscsCaseData.class);
    }
}
