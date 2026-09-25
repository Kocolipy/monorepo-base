package com.example.backend.audit;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;
import org.slf4j.event.KeyValuePair;

/**
 * Captures log records and their structured key-value pairs, so a test can assert
 * that a run reported what it did rather than only that it returned.
 *
 * <p>Attaches to the root logger: a record from any logger is captured whether or
 * not the test knew which class would emit it, which is what an assertion about a
 * log the service produces needs. The pairs are read from the record rather than
 * from rendered text, because the property being asserted is that the values are
 * named fields and not a sentence.
 */
public final class CapturedLog implements AutoCloseable {

    private final Logger root;
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    private CapturedLog(Logger root) {
        this.root = root;
    }

    public static CapturedLog attach() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        CapturedLog captured = new CapturedLog(root);
        captured.appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        captured.appender.start();
        root.addAppender(captured.appender);
        return captured;
    }

    /** Every captured record at or above {@code level} carrying {@code action}. */
    public List<ILoggingEvent> withAction(Level level, String actionKey, String action) {
        return appender.list.stream()
                .filter(record -> record.getLevel().isGreaterOrEqual(level))
                .filter(record -> action.equals(field(record, actionKey)))
                .toList();
    }

    /** One record's structured fields, as a map from key to value. */
    public static Map<String, Object> fields(ILoggingEvent record) {
        List<KeyValuePair> pairs = record.getKeyValuePairs();
        return pairs == null
                ? Map.of()
                : pairs.stream().collect(Collectors.toMap(
                        pair -> pair.key,
                        pair -> pair.value,
                        (first, second) -> second));
    }

    public void reset() {
        appender.list.clear();
    }

    @Override
    public void close() {
        root.detachAppender(appender);
        appender.stop();
    }

    private static Object field(ILoggingEvent record, String key) {
        return fields(record).get(key);
    }

    /** Convenience for reading one field off one record. */
    public static <T> T field(ILoggingEvent record, String key, Function<Object, T> as) {
        return as.apply(fields(record).get(key));
    }
}
