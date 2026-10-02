package com.example.backend.scim.config;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.example.backend.audit.CapturedLog;
import com.example.backend.observability.LogEvent;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * The filter-level SCIM refusal record: a {@code 5xx} is a classified {@code ERROR} that marks its
 * request's fault as recorded; a {@code 4xx} is an unclassified {@code WARN} that does not.
 */
class ScimErrorDocumentErrorFieldsTests {

    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/scim/v2/x");

    private CapturedLog logs;

    @BeforeEach
    void setUp() {
        logs = CapturedLog.attach();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        logs.close();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void aServerErrorIsAClassifiedErrorAndMarksTheFault() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ScimErrorDocument.write(response, 503);

        ILoggingEvent record = onlyRecord();
        assertThat(record.getLevel()).isEqualTo(Level.ERROR);
        assertThat(CapturedLog.fields(record))
                .containsEntry(LogEvent.ERROR_CODE, 503)
                .containsEntry(LogEvent.ERROR_CATEGORY, "application")
                .containsEntry(LogEvent.ERROR_FOLLOW_UP_ACTION, true)
                .containsEntry(LogEvent.REASON, "serverError");
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(faultRecorded()).isTrue();
    }

    @Test
    void aClientErrorIsUnclassifiedAndLeavesTheRequestUnmarked() throws Exception {
        ScimErrorDocument.write(new MockHttpServletResponse(), 404);

        ILoggingEvent record = onlyRecord();
        assertThat(record.getLevel()).isEqualTo(Level.WARN);
        assertThat(CapturedLog.fields(record)).doesNotContainKeys(
                LogEvent.ERROR_CODE, LogEvent.ERROR_CATEGORY, LogEvent.ERROR_FOLLOW_UP_ACTION);
        assertThat(faultRecorded()).isFalse();
    }

    private ILoggingEvent onlyRecord() {
        List<ILoggingEvent> records = logs.withAction(Level.TRACE, LogEvent.KIND, "event");
        assertThat(records).hasSize(1);
        return records.getFirst();
    }

    private boolean faultRecorded() {
        return Boolean.TRUE.equals(
                request.getAttribute("com.example.backend.observability.RequestFault.recorded"));
    }
}
