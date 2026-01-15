package uk.gov.hmcts.reform.sscs.trigger.triggers;

import uk.gov.hmcts.reform.ccd.client.model.CaseEventDetail;

import java.util.List;

public interface Trigger {

    void processCase(String caseId);
    String query();
    String event();
    boolean isValid(List<CaseEventDetail> events);

}
