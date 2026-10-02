package com.example.backend.observability;

/**
 * A fault's stand-in for the log: the original's stack, under a message that is only a type
 * name, and no cause chain — every link of which would print its own message. What makes
 * "the exception attached" compatible with "no value of the row" for a failure whose message
 * quotes what the caller sent, as a database driver's quotes the refused statement and the
 * conflicting value.
 *
 * <p>Attach it in place of the fault, never beside it; the fault itself must then reach no
 * other record.
 */
public final class RedactedFaultException extends RuntimeException {

    private RedactedFaultException(String causeType) {
        super(causeType, null, false, true);
    }

    /**
     * @param fault     the failure whose stack the record should show
     * @param causeType what the record says in place of its message: a type name, never text
     *                  the failure carried
     */
    public static RedactedFaultException of(Throwable fault, String causeType) {
        RedactedFaultException redacted = new RedactedFaultException(causeType);
        redacted.setStackTrace(fault.getStackTrace());
        return redacted;
    }
}
