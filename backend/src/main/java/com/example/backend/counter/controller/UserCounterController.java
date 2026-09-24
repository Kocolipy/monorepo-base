package com.example.backend.counter.controller;

import com.example.backend.counter.application.UserCounterService;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inbound HTTP adapter for the counter use cases.
 */
@RestController
@RequestMapping("/api/count")
public class UserCounterController {

    private final UserCounterService service;

    public UserCounterController(UserCounterService service) {
        this.service = service;
    }

    @GetMapping
    public CountResponse getCount(Principal principal) {
        return new CountResponse(service.getCount(principal.getName()));
    }

    @PostMapping("/increment")
    public CountResponse increment(Principal principal) {
        return new CountResponse(service.increment(principal.getName()));
    }

    @PostMapping("/reset")
    public CountResponse reset(Principal principal) {
        return new CountResponse(service.reset(principal.getName()));
    }

    public record CountResponse(long count) {
    }
}
