package uk.gov.hmcts.reform.sscs.trigger;

import static java.util.Objects.requireNonNull;

import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.idam.client.IdamApi;
import uk.gov.hmcts.reform.sscs.trigger.triggers.Trigger;

@Component
@Log4j2
@SpringBootApplication
@ComponentScan(basePackages = {
    "uk.gov.hmcts.reform.idam.client",
    "uk.gov.hmcts.reform.ccd.client",
    "uk.gov.hmcts.reform.sscs"})
@EnableFeignClients(clients = { IdamApi.class })
@SuppressWarnings("PMD.DoNotTerminateVM")
public class NightlyRunnerApp implements CommandLineRunner {

    private final List<Trigger> triggers;

    public NightlyRunnerApp(List<Trigger> triggers) {
        requireNonNull(triggers, "triggers must not be null");
        this.triggers = triggers;
    }

    public static void main(final String[] args) {
        SpringApplication.run(NightlyRunnerApp.class, args);
    }

    @Override
    public void run(String... args) {
        triggers.forEach(Trigger::execute);
    }
}
