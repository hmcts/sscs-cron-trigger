package uk.gov.hmcts.reform.sscs.trigger.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.TokenResponse;

import java.time.LocalDateTime;

@Component
public class AuthorisationService {

    private final IdamClient idamClient;
    private final AuthTokenGenerator s2sTokenGenerator;
    private final String systemUsername;
    private final String systemPassword;

    private String accessToken;

    private LocalDateTime tokenExpires;

    public AuthorisationService(@Value("${sscs.users.system_update_username}") String systemUsername,
                                @Value("${sscs.users.system_update_password}") String systemPassword,
                                IdamClient idamClient, AuthTokenGenerator s2sTokenGenerator) {
        this.systemUsername = systemUsername;
        this.systemPassword = systemPassword;
        this.idamClient = idamClient;
        this.s2sTokenGenerator = s2sTokenGenerator;
    }

    public String getSystemUserAccessToken() {
        if (accessToken == null || tokenExpires != null && LocalDateTime.now().isAfter(tokenExpires)) {
            TokenResponse tokenResponse = idamClient.getAccessTokenResponse(systemUsername, systemPassword);
            tokenExpires = LocalDateTime.now().plusSeconds(Long.parseLong(tokenResponse.expiresIn) - 300);
            accessToken = idamClient.BEARER_AUTH_TYPE + " " + tokenResponse.accessToken;
        }
        return accessToken;
    }

    public String getSystemUserId() {
        return idamClient.getUserInfo(getSystemUserAccessToken()).getUid();
    }

    public String getServiceToken() {
        return s2sTokenGenerator.generate();
    }
}
