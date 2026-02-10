package uk.gov.hmcts.reform.sscs.trigger;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.idam.client.IdamApi;
import uk.gov.hmcts.reform.sscs.trigger.triggers.Trigger;

import java.util.List;

import static java.util.Objects.requireNonNull;

@Component
@Log4j2
@SpringBootApplication
@ComponentScan(basePackages = {
    "uk.gov.hmcts.reform.idam.client",
    "uk.gov.hmcts.reform.ccd.client",
    "uk.gov.hmcts.reform.sscs.trigger",
    "uk.gov.hmcts.reform.sscs.utility.calendar"})
@EnableFeignClients(clients = { IdamApi.class })
public class NightlyRunnerApp implements CommandLineRunner {

    private final NightlyRunner nightlyRunner;
    private final List<Trigger> triggers;

    public NightlyRunnerApp(NightlyRunner nightlyRunner, List<Trigger> triggers) {
        this.nightlyRunner = nightlyRunner;
        requireNonNull(triggers, "triggers must not be null");
        this.triggers = triggers;
    }

    public static void main(final String[] args) {
        SpringApplication.run(NightlyRunnerApp.class, args);
    }

    @Override
    public void run(String... args) {
        triggers.forEach(trigger -> {
            try {
                log.info("Running trigger: {}", trigger.getClass().getName());
                nightlyRunner
                    .findCases(trigger.query())
                    .forEach(trigger::processCase
                    );
            } catch (Exception e) {
                log.error("Failed to execute trigger {}", getClass().getName());
                log.catching(e);
            }
        });
    }
}
