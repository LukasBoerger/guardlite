package com.guardlite.demo.controller;

import com.guardlite.demo.entities.Website;
import com.guardlite.demo.repositories.WebsiteRepository;
import com.guardlite.demo.security.UserPrincipal;
import com.guardlite.demo.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/websites")
@RequiredArgsConstructor
public class WebsiteController {
    private final WebsiteRepository websites;
    private final UserRepository users;


    @PostMapping
    public WebsiteRes create(@AuthenticationPrincipal UserPrincipal me,
                             @RequestBody CreateWebsiteReq r) {
        var owner = users.findByEmail(me.getUsername()).orElseThrow();

        var w = new Website();
        w.setId(UUID.randomUUID());
        w.setOwner(owner);
        w.setUrl(r.url());
        w.setCms(r.cms());
        w.setActive(Boolean.TRUE.equals(r.active()));
        w.setCreatedAt(Instant.now());

        websites.save(w);
        return WebsiteRes.from(w);
    }

    @GetMapping
    public List<WebsiteRes> myWebsites(@AuthenticationPrincipal UserPrincipal me) {
        var owner = users.findByEmail(me.getUsername()).orElseThrow();
        return websites.findByOwner_Id(owner.getId()).stream().map(WebsiteRes::from).toList();
    }

    public record CreateWebsiteReq(String url, String cms, Boolean active) {
    }

    public record WebsiteRes(UUID id, String url, String cms, boolean active, Instant createdAt) {
        static WebsiteRes from(Website w) {
            return new WebsiteRes(w.getId(), w.getUrl(), w.getCms(), w.isActive(), w.getCreatedAt());
        }
    }
}