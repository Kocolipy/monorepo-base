package com.example.backend.scim.config;

import java.io.IOException;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * The filter-level SCIM fault record, written for {@code EcsLogFormatTests}, which reads the
 * encoded stream from another package. No route produces a {@code 5xx} at this layer on demand.
 */
public final class ScimErrorDocumentRecords {

    private ScimErrorDocumentRecords() {
    }

    public static void serverError() throws IOException {
        ScimErrorDocument.write(new MockHttpServletResponse(), 500);
    }
}
