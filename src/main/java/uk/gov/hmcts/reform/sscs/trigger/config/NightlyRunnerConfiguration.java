package uk.gov.hmcts.reform.sscs.trigger.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

@Configuration
public class NightlyRunnerConfiguration {

    @Bean
    public LocalDate triggerDate(@Value("${trigger.date:#{null}}") String triggerDate) {
        if (triggerDate != null) {
            return LocalDate.parse(triggerDate);
        }
        return LocalDate.now();
    }

}
