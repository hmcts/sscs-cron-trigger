package uk.gov.hmcts.reform.sscs.trigger.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.idam.client.IdamApi;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.OAuth2Configuration;
import uk.gov.hmcts.reform.idam.client.models.TokenResponse;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthorisationServiceTest {

    private static final String USERNAME = "system.update";
    private static final String PASSWORD = "password";
    public static final String ACCESS_TOKEN_1 = "ACCESS-TOKEN-1";
    public static final String ACCESS_TOKEN_2 = "ACCESS-TOKEN-2";
    public static final String EXPIRES_IN = "250";
    public static final String ID_TOKEN = "id_token";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String SCOPE = "scope";
    public static final String TOKEN_TYPE = "token_type";
    public static final String BEARER_ACCESS_TOKEN_1 = "Bearer " + ACCESS_TOKEN_1;
    public static final String BEARER_ACCESS_TOKEN_2 = "Bearer " + ACCESS_TOKEN_2;
    public static final String SYSTEM_USER_UID = "SYSTEM-USER-UID";
    public static final String SERVICE_TOKEN = "SERVICE-TOKEN";

    @Mock
    private IdamApi idamApi;
    @Mock
    private OAuth2Configuration oauth2Configuration;
    private IdamClient idamClient;
    private AuthorisationService authorisationService;

    @BeforeEach
    void initMocks() {
        MockitoAnnotations.initMocks(this);
        idamClient = Mockito.spy(new IdamClient(idamApi, oauth2Configuration));
        AuthTokenGenerator s2sTokenGenerator = () -> SERVICE_TOKEN;
        authorisationService = new AuthorisationService(USERNAME, PASSWORD, idamClient, s2sTokenGenerator);
    }

    @Test
    void returnCachedTokenWhenAvailableAndValid() {
        when(idamClient.getAccessTokenResponse(eq(USERNAME), eq(PASSWORD))).thenReturn(
            new TokenResponse(ACCESS_TOKEN_1, "3600", ID_TOKEN, REFRESH_TOKEN, SCOPE, TOKEN_TYPE));

        assertEquals(BEARER_ACCESS_TOKEN_1, authorisationService.getSystemUserAccessToken(),
                     "Should return new access token");
        assertEquals(BEARER_ACCESS_TOKEN_1, authorisationService.getSystemUserAccessToken(),
                     "Should return cached access token");

        verify(idamClient, times(1)).getAccessTokenResponse(anyString(), anyString());
    }

    @Test
    void returnNewTokenWhenCachedTokenHasExpired() {
        when(idamClient.getAccessTokenResponse(eq(USERNAME), eq(PASSWORD)))
            .thenReturn(new TokenResponse(ACCESS_TOKEN_1, EXPIRES_IN, ID_TOKEN, REFRESH_TOKEN, SCOPE, TOKEN_TYPE))
            .thenReturn(new TokenResponse(ACCESS_TOKEN_2, EXPIRES_IN, ID_TOKEN, REFRESH_TOKEN, SCOPE, TOKEN_TYPE));

        assertEquals(BEARER_ACCESS_TOKEN_1, authorisationService.getSystemUserAccessToken(),
                     "Should return new access token");
        assertEquals(BEARER_ACCESS_TOKEN_2, authorisationService.getSystemUserAccessToken(),
                     "Should return new access token");

        verify(idamClient, times(2)).getAccessTokenResponse(anyString(), anyString());
    }

    @Test
    void returnsSystemUserId() {
        when(idamClient.getAccessTokenResponse(eq(USERNAME), eq(PASSWORD))).thenReturn(
            new TokenResponse(ACCESS_TOKEN_1, "3600", ID_TOKEN, REFRESH_TOKEN, SCOPE, TOKEN_TYPE));

        when(idamClient.getUserInfo(eq(BEARER_ACCESS_TOKEN_1))).thenReturn(
            UserInfo.builder().uid(SYSTEM_USER_UID).build());

        assertEquals(SYSTEM_USER_UID, authorisationService.getSystemUserId(), "Should return correct user uid");
    }

    @Test
    void returnsServiceToken() {
        assertEquals(SERVICE_TOKEN, authorisationService.getServiceToken(), "Should return service token");
    }
}
