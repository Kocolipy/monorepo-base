package com.example.backend.observability;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import java.lang.reflect.Field;
import java.util.Iterator;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * The child process {@link LogFileTests} launches: a Spring application with no beans of
 * its own, so the only thing it does is what every start of this service does first —
 * load the configuration and initialize logging from it. It emits one record, then
 * reports the file appender's rolling policy on stdout, prefixed {@value #POLICY}, so the
 * parent can read the settings logging actually ended up with rather than the YAML that
 * asked for them.
 *
 * <p>A separate process because logging is JVM-global: initializing it with a log file in
 * the test JVM would reconfigure every cached test context's logging along with it.
 */
public final class LogFileProbe {

    static final String MESSAGE = "Log file probe record";

    static final String POLICY = "PROBE-POLICY ";

    private LogFileProbe() {
    }

    public static void main(String[] args) throws Exception {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(LogFileProbe.class)
                .web(WebApplicationType.NONE)
                .run(args)) {
            LoggerFactory.getLogger(LogFileProbe.class).info(MESSAGE);
            reportRollingPolicy();
        }
    }

    private static void reportRollingPolicy() throws ReflectiveOperationException {
        LoggerContext logging = (LoggerContext) LoggerFactory.getILoggerFactory();
        Iterator<Appender<ch.qos.logback.classic.spi.ILoggingEvent>> appenders =
                logging.getLogger(Logger.ROOT_LOGGER_NAME).iteratorForAppenders();
        while (appenders.hasNext()) {
            if (appenders.next() instanceof RollingFileAppender<?> file
                    && file.getRollingPolicy() instanceof TimeBasedRollingPolicy<?> policy) {
                System.out.println(POLICY + "class=" + policy.getClass().getName()
                        + " pattern=" + policy.getFileNamePattern()
                        + " maxHistory=" + policy.getMaxHistory()
                        + " maxFileSize=" + bytes(policy, "maxFileSize")
                        + " totalSizeCap=" + bytes(policy, "totalSizeCap"));
            }
        }
    }

    /** Logback exposes these two only as setters, so they are read from their fields. */
    private static long bytes(Object policy, String name) throws ReflectiveOperationException {
        for (Class<?> type = policy.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getName().equals(name) && field.getType() == FileSize.class) {
                    field.setAccessible(true);
                    return ((FileSize) field.get(policy)).getSize();
                }
            }
        }
        throw new NoSuchFieldException(name);
    }
}
