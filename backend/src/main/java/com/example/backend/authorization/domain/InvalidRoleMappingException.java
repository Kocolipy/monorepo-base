package com.example.backend.authorization.domain;

import java.util.List;

/**
 * A role mapping the application refuses to start with. The message names every problem found,
 * each one on its own, so the operator can fix them all in one redeploy.
 */
public class InvalidRoleMappingException extends IllegalArgumentException {

    private final List<String> problems;

    public InvalidRoleMappingException(List<String> problems) {
        super("Invalid role mapping: " + String.join("; ", problems));
        this.problems = List.copyOf(problems);
    }

    /** Each problem, as it appears in the message. */
    public List<String> problems() {
        return problems;
    }
}
