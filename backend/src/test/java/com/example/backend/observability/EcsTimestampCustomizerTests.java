package com.example.backend.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.json.JsonWriter.Members;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The timestamp rendering on its own, through Boot's own {@link JsonWriter}: which member it
 * rewrites, what it writes there, and that it touches nothing else.
 * {@code EcsLogFormatTests} holds the same property end to end, against real records.
 */
class EcsTimestampCustomizerTests {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** 2026-10-01 08:52:11.700 UTC, which is 16:52:11.700 in Singapore. */
    private static final Instant INSTANT = Instant.parse("2026-10-01T08:52:11.700Z");

    @Test
    void the_timestamp_is_the_same_instant_written_at_plus_eight_with_three_digit_millis() {
        JsonNode record = write(INSTANT);

        assertThat(record.at("/@timestamp").asText()).isEqualTo("2026-10-01T16:52:11.700+08:00");
        assertThat(Instant.parse(record.at("/@timestamp").asText())).isEqualTo(INSTANT);
    }

    /** Across midnight UTC the date moves too: the whole value is local, not only the hour. */
    @Test
    void the_date_is_the_singapore_date() {
        assertThat(write(Instant.parse("2026-09-30T20:00:00Z")).at("/@timestamp").asText())
                .isEqualTo("2026-10-01T04:00:00.000+08:00");
    }

    /** Only the top-level {@code @timestamp}: another instant in the record keeps its UTC form. */
    @Test
    void no_other_member_is_rewritten() {
        JsonNode record = write(INSTANT);

        assertThat(record.at("/other").asText()).isEqualTo("2026-10-01T08:52:11.700Z");
        assertThat(record.at("/nested/@timestamp").asText()).isEqualTo("2026-10-01T08:52:11.700Z");
    }

    @Test
    void a_value_that_is_not_an_instant_is_returned_unchanged() {
        Object value = "2026-10-01T08:52:11.700Z";

        assertThat(EcsTimestampCustomizer.render(value)).isSameAs(value);
        assertThat(EcsTimestampCustomizer.render(null)).isNull();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static JsonNode write(Instant instant) {
        JsonWriter<Instant> writer = JsonWriter.of(members -> {
            members.add("@timestamp", value -> value);
            members.add("other", value -> value);
            members.add("nested").usingMembers(nested -> nested.add("@timestamp", value -> value));
            new EcsTimestampCustomizer().customize((Members) members);
        });
        return JSON.readTree(writer.writeToString(instant));
    }
}
