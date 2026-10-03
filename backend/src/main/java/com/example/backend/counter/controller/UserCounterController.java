package com.example.backend.counter.controller;

import com.example.backend.counter.application.UserCounterService;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inbound HTTP adapter for the counter use cases.
 *
 * <p>The scheme's ordinary, non-administrative example: reading the counter requires
 * {@code counter:read} and changing it {@code counter:write}, declared on each handler with
 * method security and repeated by the chain as a backstop.
 */
@RestController
@RequestMapping("/api/count")
public class UserCounterController {

    private final UserCounterService service;

    public UserCounterController(UserCounterService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('counter:read')")
    public CountResponse getCount(Principal principal) {
        return new CountResponse(service.getCount(principal.getName()));
    }

    @PostMapping("/increment")
    @PreAuthorize("hasAuthority('counter:write')")
    public CountResponse increment(Principal principal) {
        return new CountResponse(service.increment(principal.getName()));
    }

    @PostMapping("/reset")
    @PreAuthorize("hasAuthority('counter:write')")
    public CountResponse reset(Principal principal) {
        return new CountResponse(service.reset(principal.getName()));
    }

    public record CountResponse(long count) {
    }
}
