package com.guardlite.demo.controller;

import com.guardlite.demo.entities.Check;
import com.guardlite.demo.repositories.CheckRepository;
import com.guardlite.demo.repositories.CheckResultRepository;
import com.guardlite.demo.repositories.WebsiteRepository;
import com.guardlite.demo.security.UserPrincipal;
import com.guardlite.demo.services.CheckRunner;
import com.guardlite.demo.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CheckController {

    private final WebsiteRepository websites;
    private final CheckRepository checks;
    private final CheckResultRepository results;
    private final UserRepository users;
    private final CheckRunner runner;

    @PostMapping("/websites/{websiteId}/checks")
    public CheckRes create(@AuthenticationPrincipal UserPrincipal me,
                           @PathVariable UUID websiteId,
                           @RequestBody CreateCheckReq r) {
        if (r.cadenceCron() == null || !CronExpression.isValidExpression(r.cadenceCron())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_cron");
        }

        var owner = users.findByEmail(me.getUsername()).orElseThrow();
        var w = websites.findByIdAndOwner_Id(websiteId, owner.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        var c = new Check();
        c.setWebsite(w);
        c.setType(r.type() == null ? "HTTP_UP" : r.type());
        c.setCadenceCron(r.cadenceCron());
        c.setEnabled(Boolean.TRUE.equals(r.enabled()));
        c.setLastRunAt(null); // first run steht aus

        checks.save(c);
        return CheckRes.of(c);
    }

    @GetMapping("/websites/{websiteId}/checks")
    public List<CheckRes> list(@AuthenticationPrincipal UserPrincipal me, @PathVariable UUID websiteId) {
        var owner = users.findByEmail(me.getUsername()).orElseThrow();
        websites.findByIdAndOwner_Id(websiteId, owner.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        return checks.findByWebsite_Id(websiteId).stream().map(CheckRes::of).toList();
    }

    // manueller Run
    @PostMapping("/checks/{checkId}/run")
    public ResponseEntity<Void> runOnce(@AuthenticationPrincipal UserPrincipal me, @PathVariable UUID checkId) {
        var owner = users.findByEmail(me.getUsername()).orElseThrow();
        var c = checks.findByIdAndWebsite_Owner_Id(checkId, owner.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        runner.runOnce(c);
        c.setLastRunAt(Instant.now());
        checks.save(c);
        return ResponseEntity.accepted().build();
    }

    // DTOs
    public record CreateCheckReq(String type, String cadenceCron, Boolean enabled) {
    }

    public record CheckRes(UUID id, UUID websiteId, String type, String cadenceCron, boolean enabled,
                           Instant lastRunAt) {
        static CheckRes of(Check c) {
            return new CheckRes(
                    c.getId(),
                    c.getWebsite().getId(),
                    c.getType(),
                    c.getCadenceCron(),
                    c.isEnabled(),
                    c.getLastRunAt()
            );
        }
    }
}

