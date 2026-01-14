package uk.gov.hmcts.reform.sscs.trigger.config;

import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

@Configuration
@Log4j2
public class NightlyRunnerConfiguration {

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient();
    }


    @Bean
    public LocalDate triggerDate(@Value("${trigger.date:#{null}}") String triggerDate) {
        if (StringUtils.isNoneBlank(triggerDate)) {
            LocalDate date = LocalDate.parse(triggerDate);
            log.info("Trigger date: {}", triggerDate);
            return date;
        }
        return LocalDate.now();
    }

}
