package uk.gov.hmcts.reform.sscs.trigger;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class NightlyRunnerAppIntegrationTest {

    private static final String SYSTEM_USERNAME = "system.update@example.com";
    private static final String SYSTEM_PASSWORD = "system-password";
    private static final String SYSTEM_USER_ID = "system-user-id";
    private static final String ACCESS_TOKEN = "system-user-token";
    private static final String USER_TOKEN = "Bearer " + ACCESS_TOKEN;
    private static final String MICROSERVICE = "sscs";
    private static final String S2S_JWT = jwt();
    private static final String S2S_TOKEN = "Bearer " + S2S_JWT;
    private static final String TRIGGER_DATE = "2026-01-15";
    private static final String CASE_ID = "1001";
    private static final String ALREADY_TRIGGERED_TODAY_CASE_ID = "1002";
    private static final String EVENT_ID = "hearingToday";
    private static final String CASES_PATH = "/caseworkers/"
        + SYSTEM_USER_ID
        + "/jurisdictions/SSCS/case-types"
        + "/Benefit/cases/";
    private static final String CASE_PATH = CASES_PATH + CASE_ID;
    private static final String ALREADY_TRIGGERED_TODAY_CASE_PATH = CASES_PATH + ALREADY_TRIGGERED_TODAY_CASE_ID;

    private static final WireMockServer WIRE_MOCK = new WireMockServer(wireMockConfig().dynamicPort());
    private static final String EVENTS = "/events";

    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("core_case_data.api.url", WIRE_MOCK::baseUrl);
        registry.add("idam.api.url", WIRE_MOCK::baseUrl);
        registry.add("idam.s2s-auth.url", WIRE_MOCK::baseUrl);
        registry.add("idam.s2s-auth.microservice", () -> MICROSERVICE);
        registry.add("idam.client.id", () -> "sscs");
        registry.add("idam.client.secret", () -> "client-secret");
        registry.add("idam.client.redirect_uri", () -> "http://localhost/redirect");
        registry.add("sscs.users.system_update_username", () -> SYSTEM_USERNAME);
        registry.add("sscs.users.system_update_password", () -> SYSTEM_PASSWORD);
        registry.add("trigger.date", () -> TRIGGER_DATE);
    }

    @BeforeAll
    static void startStubs() {
        WIRE_MOCK.start();

        WIRE_MOCK.stubFor(post(urlPathEqualTo("/lease"))
            .willReturn(ok(S2S_JWT).withHeader("Content-Type", "text/plain")));

        WIRE_MOCK.stubFor(post(urlPathEqualTo("/o/token"))
            .willReturn(okJson("{\"access_token\":\""
                + ACCESS_TOKEN
                + "\",\"expires_in\":\"28800\","
                + "\"id_token\":\"id-token\","
                + "\"refresh_token\":\"refresh-token\","
                + "\"scope\":\"openid profile roles\","
                + "\"token_type\":\"Bearer\"}")));

        WIRE_MOCK.stubFor(get(urlPathEqualTo("/o/userinfo"))
            .withHeader("Authorization", equalTo(USER_TOKEN))
            .willReturn(okJson("{\"sub\":\""
                + SYSTEM_USERNAME
                + "\",\"uid\":\""
                + SYSTEM_USER_ID
                + "\"}")));

        WIRE_MOCK.stubFor(post(urlPathEqualTo("/searchCases"))
            .withRequestBody(matchingJsonPath(
                "$.query.match['data.hearings.value.hearingDate']",
                equalTo(TRIGGER_DATE)
            ))
            .willReturn(okJson("{\"total\":2,\"cases\":[{\"id\":"
                + CASE_ID
                + "},{\"id\":"
                + ALREADY_TRIGGERED_TODAY_CASE_ID
                + "}]}")));

        WIRE_MOCK.stubFor(get(urlEqualTo(CASE_PATH + EVENTS))
            .willReturn(okJson("[{\"id\":\"appealCreated\",\"created_date\":\"2026-01-10T09:00:00\"},"
                + "{\"id\":\""
                + EVENT_ID
                + "\","
                + "\"created_date\":\"2026-01-14T23:59:59\"}]")));

        WIRE_MOCK.stubFor(get(urlEqualTo(ALREADY_TRIGGERED_TODAY_CASE_PATH + EVENTS))
            .willReturn(okJson("[{\"id\":\""
                + EVENT_ID
                + "\",\"created_date\":\"2026-01-15T00:30:00\"}]")));

        WIRE_MOCK.stubFor(get(urlEqualTo(CASE_PATH + "/event-triggers/" + EVENT_ID + "/token"))
            .willReturn(okJson("{\"token\":\"" + EVENT_ID + "-token\",\"event_id\":\"" + EVENT_ID + "\"}")));

        WIRE_MOCK.stubFor(post(urlPathEqualTo(CASE_PATH + EVENTS))
            .willReturn(okJson("{\"id\":" + CASE_ID + "}")));
    }

    @AfterAll
    static void stopStubs() {
        WIRE_MOCK.stop();
    }

    @Test
    void shouldLeaseServiceTokenFromS2sUsingMicroserviceAndOneTimePassword() {
        final List<LoggedRequest> leases = WIRE_MOCK.findAll(postRequestedFor(urlPathEqualTo("/lease"))
            .withRequestBody(matchingJsonPath("$.microservice", equalTo(MICROSERVICE)))
            .withRequestBody(matchingJsonPath("$.oneTimePassword", matching("\\d+"))));

        assertThat(leases).hasSize(1);
    }

    @Test
    void shouldRequestSystemUserTokenAndUserInfoFromIdam() {
        final List<LoggedRequest> tokenRequests = WIRE_MOCK.findAll(postRequestedFor(urlPathEqualTo("/o/token"))
            .withFormParam("grant_type", equalTo("password"))
            .withFormParam("username", equalTo(SYSTEM_USERNAME))
            .withFormParam("password", equalTo(SYSTEM_PASSWORD))
            .withFormParam("client_id", equalTo("sscs"))
            .withFormParam("client_secret", equalTo("client-secret")));

        assertThat(tokenRequests).hasSize(1);
        assertThat(WIRE_MOCK.findAll(getRequestedFor(urlPathEqualTo("/o/userinfo")))).hasSize(1);
    }

    @Test
    void shouldSearchCcdOncePerTriggerWithSystemUserAndServiceTokens() {
        final List<LoggedRequest> searches = WIRE_MOCK.findAll(postRequestedFor(urlPathEqualTo("/searchCases"))
            .withQueryParam("ctid", equalTo("Benefit")));

        assertThat(searches).hasSize(3);
        assertThat(searches).allSatisfy(request -> {
            assertThat(request.getHeader("Authorization")).isEqualTo(USER_TOKEN);
            assertThat(request.getHeader("ServiceAuthorization")).isEqualTo(S2S_TOKEN);
        });
    }

    @Test
    void shouldReadEventHistoryForEachMatchedCase() {
        assertThat(WIRE_MOCK.findAll(getRequestedFor(urlEqualTo(CASE_PATH + EVENTS)))).hasSize(1);
        assertThat(WIRE_MOCK.findAll(getRequestedFor(urlEqualTo(ALREADY_TRIGGERED_TODAY_CASE_PATH + EVENTS))))
            .hasSize(1);
    }

    @Test
    void shouldSubmitHearingTodayEventForCaseWithHearingToday() {
        assertThat(WIRE_MOCK.findAll(getRequestedFor(urlEqualTo(
            CASE_PATH + "/event-triggers/" + EVENT_ID + "/token"
        )))).hasSize(1);

        final List<LoggedRequest> submissions = WIRE_MOCK.findAll(postRequestedFor(urlPathEqualTo(CASE_PATH + EVENTS))
            .withQueryParam("ignore-warning", equalTo("true"))
            .withRequestBody(matchingJsonPath("$.event.id", equalTo(EVENT_ID)))
            .withRequestBody(matchingJsonPath("$.event_token", equalTo(EVENT_ID + "-token")))
            .withRequestBody(matchingJsonPath("$.case_reference", equalTo(CASE_ID))));

        assertThat(submissions).hasSize(1);
    }

    @Test
    void shouldNotSubmitHearingTodayEventForCaseAlreadyTriggeredToday() {
        assertThat(WIRE_MOCK.findAll(getRequestedFor(urlMatching(
            ALREADY_TRIGGERED_TODAY_CASE_PATH + "/event-triggers/.*"
        )))).isEmpty();
        assertThat(WIRE_MOCK.findAll(postRequestedFor(urlPathEqualTo(
            ALREADY_TRIGGERED_TODAY_CASE_PATH + EVENTS
        )))).isEmpty();
    }

    private static String jwt() {
        final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        final String header = encoder.encodeToString(
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8)
        );
        final String payload = encoder.encodeToString(("{\"sub\":\""
            + MICROSERVICE
            + "\",\"exp\":4102444800}").getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".c2lnbmF0dXJl";
    }
}
