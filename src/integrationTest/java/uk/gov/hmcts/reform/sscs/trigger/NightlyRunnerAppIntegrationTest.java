package uk.gov.hmcts.reform.sscs.trigger;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;
import uk.gov.hmcts.reform.sscs.trigger.service.AuthorisationService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class NightlyRunnerAppIntegrationTest {

  private static final String USER_TOKEN = "Bearer system-user-token";
  private static final String S2S_TOKEN = "s2s-token";
  private static final String CASE_ID = "1001";
  private static final String EVENT_ID = "hearingToday";
  private static final String CASE_PATH =
      "/caseworkers/system-user-id/jurisdictions/SSCS/case-types/Benefit/cases/" + CASE_ID;

  private static final WireMockServer WIRE_MOCK = new WireMockServer(wireMockConfig().dynamicPort());

  @TestBean
  private AuthorisationService authorisationService;

  static AuthorisationService authorisationService() {
    final AuthorisationService authorisationService = mock(AuthorisationService.class);
    when(authorisationService.getSystemUserAccessToken()).thenReturn(USER_TOKEN);
    when(authorisationService.getServiceToken()).thenReturn(S2S_TOKEN);
    when(authorisationService.getSystemUserId()).thenReturn("system-user-id");
    return authorisationService;
  }

  @DynamicPropertySource
  static void properties(final DynamicPropertyRegistry registry) {
    registry.add("core_case_data.api.url", WIRE_MOCK::baseUrl);
    registry.add("idam.client.secret", () -> "client-secret");
    registry.add("idam.client.redirect_uri", () -> "http://localhost/redirect");
    registry.add("trigger.date", () -> "2026-01-15");
  }

  @BeforeAll
  static void startStubs() {
    WIRE_MOCK.start();

    WIRE_MOCK.stubFor(
        post(urlPathEqualTo("/searchCases"))
            .withRequestBody(matchingJsonPath("$.query.match['data.hearings.value.hearingDate']", equalTo("2026-01-15")))
            .willReturn(okJson("{\"total\":1,\"cases\":[{\"id\":" + CASE_ID + "}]}")));

    WIRE_MOCK.stubFor(
        get(urlEqualTo(CASE_PATH + "/event-triggers/" + EVENT_ID + "/token"))
            .willReturn(okJson("{\"token\":\"" + EVENT_ID + "-token\",\"event_id\":\"" + EVENT_ID + "\"}")));

    WIRE_MOCK.stubFor(post(urlPathEqualTo(CASE_PATH + "/events")).willReturn(okJson("{\"id\":" + CASE_ID + "}")));
  }

  @AfterAll
  static void stopStubs() {
    WIRE_MOCK.stop();
  }

  @Test
  void shouldSearchCcdOncePerTriggerWithSystemUserAndServiceTokens() {
    final List<LoggedRequest> searches =
        WIRE_MOCK.findAll(postRequestedFor(urlPathEqualTo("/searchCases")).withQueryParam("ctid", equalTo("Benefit")));

    assertThat(searches).hasSize(3);
    assertThat(searches)
        .allSatisfy(request -> {
          assertThat(request.getHeader("Authorization")).isEqualTo(USER_TOKEN);
          assertThat(request.getHeader("ServiceAuthorization")).isEqualTo(S2S_TOKEN);
        });
  }

  @Test
  void shouldSubmitHearingTodayEventForCaseWithHearingToday() {
    assertThat(WIRE_MOCK.findAll(getRequestedFor(urlEqualTo(CASE_PATH + "/event-triggers/" + EVENT_ID + "/token"))))
        .hasSize(1);

    final List<LoggedRequest> submissions =
        WIRE_MOCK.findAll(
            postRequestedFor(urlPathEqualTo(CASE_PATH + "/events"))
                .withQueryParam("ignore-warning", equalTo("true"))
                .withRequestBody(matchingJsonPath("$.event.id", equalTo(EVENT_ID)))
                .withRequestBody(matchingJsonPath("$.event_token", equalTo(EVENT_ID + "-token")))
                .withRequestBody(matchingJsonPath("$.case_reference", equalTo(CASE_ID))));

    assertThat(submissions).hasSize(1);
  }
}
