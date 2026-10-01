package com.example.backend.observability;

import java.time.ZoneId;

/**
 * The zone an operator reads this service in: Singapore time, UTC+8, as the logging
 * standard requires. Two things are stated in it, and they are stated in the same zone
 * so they agree — a log record's {@code @timestamp} ({@link EcsTimestampCustomizer}) and
 * the cron expression a scheduled job runs on. A job scheduled for 03:30 runs at the
 * 03:30 its own records show.
 *
 * <p>Deliberately not the JVM's default zone, which this service never sets. Moving the
 * default would move everything that reads it — SCIM {@code meta} times, audit times and
 * the injected {@link java.time.Clock} — and those stay UTC on the wire.
 */
public final class ServiceTimeZone {

    /** {@code Asia/Singapore}: a fixed +08:00, with no daylight saving. */
    public static final ZoneId ZONE = ZoneId.of("Asia/Singapore");

    private ServiceTimeZone() {
    }
}
