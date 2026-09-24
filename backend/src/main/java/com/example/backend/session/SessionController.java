package com.example.backend.session;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/session")
public class SessionController {

    private static final String DISPLAY_NAME = "displayName";

    @GetMapping
    public SessionResponse getSession(HttpSession session) {
        return toResponse(session);
    }

    @PutMapping
    public SessionResponse updateSession(
            @Valid @RequestBody UpdateSessionRequest body,
            HttpSession session) {
        session.setAttribute(DISPLAY_NAME, body.displayName());
        return toResponse(session);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    private SessionResponse toResponse(HttpSession session) {
        return new SessionResponse(
                session.getId(),
                (String) session.getAttribute(DISPLAY_NAME),
                Instant.ofEpochMilli(session.getCreationTime()),
                session.getMaxInactiveInterval());
    }

    public record UpdateSessionRequest(
            @NotBlank @Size(max = 100) String displayName) {
    }

    public record SessionResponse(
            String id,
            String displayName,
            Instant createdAt,
            int maxInactiveIntervalSeconds) {
    }
}
