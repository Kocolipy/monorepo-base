package com.example.backend.audit.domain;

/** An audit listing request whose page or page size is out of range. */
public class InvalidAuditQueryException extends RuntimeException {

    public InvalidAuditQueryException(String message) {
        super(message);
    }
}
