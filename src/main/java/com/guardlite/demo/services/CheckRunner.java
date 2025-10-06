package com.guardlite.demo.services;

import com.guardlite.demo.entities.Check;
import com.guardlite.demo.entities.CheckResult;
import com.guardlite.demo.repositories.CheckRepository;
import com.guardlite.demo.repositories.CheckResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckRunner {
    private static final int DEFAULT_TIMEOUT_MS = 3000;
    private final CheckRepository checks;
    private final CheckResultRepository results;
    private final WebClient client = WebClient.builder().build();

    @Scheduled(fixedDelay = 5000)
    public void tick() {
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        for (var c : checks.findAll()) {
            if (!c.isEnabled()) continue;

            var cron = CronExpression.parse(c.getCadenceCron());
            var last = Optional.ofNullable(c.getLastRunAt())
                    .map(t -> ZonedDateTime.ofInstant(t, ZoneOffset.UTC))
                    .orElse(now.minusYears(10));

            var next = cron.next(last);
            if (next != null && !next.isAfter(now)) {
                runOnce(c);
                c.setLastRunAt(Instant.now());   // im DB-Record merken
                checks.save(c);
            }
        }
    }

    public void runOnce(Check c) {
        var start = Instant.now();
        Integer httpStatus = null;
        String err = null;

        try {
            // type aktuell ignoriert/immer GET; später je nach type handeln
            httpStatus = client.get()
                    .uri(c.getWebsite().getUrl())
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofMillis(DEFAULT_TIMEOUT_MS))
                    .block()
                    .getStatusCode().value();
        } catch (Exception e) {
            err = e.getClass().getSimpleName() + ": " + e.getMessage();
        }

        // Status mappen
        String status = (err != null) ? "RED"
                : (httpStatus >= 200 && httpStatus < 300) ? "GREEN"
                : (httpStatus >= 300 && httpStatus < 400) ? "YELLOW"
                : "RED";

        int durationMs = (int) Duration.between(start, Instant.now()).toMillis();
        String payload = String.format(
                "{\"httpStatus\":%s,\"durationMs\":%d,\"error\":%s}",
                httpStatus == null ? "null" : httpStatus.toString(),
                durationMs,
                err == null ? "null" : ("\"" + err.replace("\"", "\\\"") + "\"")
        );

        var r = new CheckResult();
        r.setId(UUID.randomUUID());         // falls du kein @GeneratedValue hast
        r.setCheck(c);
        r.setRunAt(start);
        r.setStatus(status);
        r.setPayloadJson(payload);
        r.setAdviceText(err != null ? "Endpoint nicht erreichbar" : null);
        results.save(r);
    }
}

