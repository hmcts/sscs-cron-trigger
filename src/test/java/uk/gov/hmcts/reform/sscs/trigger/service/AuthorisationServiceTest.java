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


public class AuthorisationServiceTest {

    private static final String USERNAME = "system.update";

    private static final String PASSWORD = "password";

    @Mock
    private IdamApi idamApi;
    @Mock
    private OAuth2Configuration oauth2Configuration;
    private IdamClient idamClient;
    private AuthTokenGenerator s2sTokenGenerator;

    private AuthorisationService authorisationService;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        idamClient = Mockito.spy(new IdamClient(idamApi, oauth2Configuration));
        s2sTokenGenerator = () -> "SERVICE-TOKEN";
        authorisationService = new AuthorisationService(USERNAME, PASSWORD, idamClient, s2sTokenGenerator);
    }

    @Test
    void multipleAccessTokenRequests_returnCachedToken() {
        when(idamClient.getAccessTokenResponse(eq(USERNAME), eq(PASSWORD))).thenReturn(
            new TokenResponse("ACCESS-TOKEN","3600","id_token","refresh_token","scope", "token_type"));

        assertEquals("Bearer ACCESS-TOKEN", authorisationService.getSystemUserAccessToken());
        assertEquals("Bearer ACCESS-TOKEN", authorisationService.getSystemUserAccessToken());

        verify(idamClient, times(1)).getAccessTokenResponse(anyString(), anyString());
    }

    @Test
    void multipleAccessTokenRequests_cachedTokenExpired_returnNewToken() {
        when(idamClient.getAccessTokenResponse(eq(USERNAME), eq(PASSWORD)))
            .thenReturn(new TokenResponse("ACCESS-TOKEN-1","250","id_token","refresh_token","scope", "token_type"))
            .thenReturn(new TokenResponse("ACCESS-TOKEN-2","250","id_token","refresh_token","scope", "token_type"));

        assertEquals("Bearer ACCESS-TOKEN-1", authorisationService.getSystemUserAccessToken());
        assertEquals("Bearer ACCESS-TOKEN-2", authorisationService.getSystemUserAccessToken());

        verify(idamClient, times(2)).getAccessTokenResponse(anyString(), anyString());
    }

    @Test
    void returnsSystemUserId() {
        when(idamClient.getAccessTokenResponse(eq(USERNAME), eq(PASSWORD))).thenReturn(
            new TokenResponse("ACCESS-TOKEN","3600","id_token","refresh_token","scope", "token_type"));

        when(idamClient.getUserInfo(eq("Bearer ACCESS-TOKEN"))).thenReturn(
            UserInfo.builder().uid("SYSTEM-USER-UID").build());

        assertEquals("SYSTEM-USER-UID", authorisationService.getSystemUserId());
    }

    @Test
    void returnsServiceToken() {
        assertEquals("SERVICE-TOKEN", authorisationService.getServiceToken());
    }
}
