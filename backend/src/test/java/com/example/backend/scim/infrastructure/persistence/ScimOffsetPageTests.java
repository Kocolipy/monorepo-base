package com.example.backend.scim.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * The offset/limit {@link Pageable} SCIM paging needs, including the navigation methods nothing
 * in this service calls.
 *
 * <p>Those navigation methods are why this test exists. They are obligations of the
 * {@link Pageable} interface, so they cannot be deleted the way an unused accessor can, and
 * nothing in the application reaches them — SCIM paging is stateless and the client computes its
 * own next {@code startIndex}. That left every one of them unreached by any test, which means
 * their arithmetic was asserted by nobody while the class's own javadoc claimed it was
 * "consistent rather than correct-by-accident". This test is what makes that claim true.
 *
 * <p>The cases are chosen where the arithmetic can actually be wrong: an offset that is not a
 * multiple of the limit (so a page NUMBER cannot represent it, which is the whole reason this
 * class exists rather than {@code PageRequest}), and a previous-page step from an offset smaller
 * than one page, where subtracting would underflow past zero.
 */
class ScimOffsetPageTests {

    private static final Sort SORT = Sort.by("id");

    /** The three methods Spring Data turns into firstResult, maxResults and ORDER BY. */
    @Test
    void the_load_bearing_values_are_the_offset_the_limit_and_the_sort() {
        ScimOffsetPage page = ScimOffsetPage.of(7, 100, SORT);

        assertThat(page.getOffset()).isEqualTo(7);
        assertThat(page.getPageSize()).isEqualTo(100);
        assertThat(page.getSort()).isEqualTo(SORT);
    }

    /**
     * An arbitrary offset has no exact page number, and the reported one is the page it falls
     * WITHIN rather than a rounded-up neighbour. Asserted at an offset that is not a multiple of
     * the limit, because a multiple cannot distinguish truncation from rounding.
     */
    @Test
    void the_page_number_is_the_page_the_offset_falls_within() {
        assertThat(ScimOffsetPage.of(0, 10, SORT).getPageNumber()).isZero();
        assertThat(ScimOffsetPage.of(9, 10, SORT).getPageNumber()).isZero();
        assertThat(ScimOffsetPage.of(10, 10, SORT).getPageNumber()).isEqualTo(1);
        assertThat(ScimOffsetPage.of(25, 10, SORT).getPageNumber()).isEqualTo(2);
    }

    @Test
    void the_next_page_advances_by_exactly_one_limit_and_keeps_the_sort() {
        Pageable next = ScimOffsetPage.of(7, 100, SORT).next();

        assertThat(next.getOffset()).isEqualTo(107);
        assertThat(next.getPageSize()).isEqualTo(100);
        assertThat(next.getSort()).isEqualTo(SORT);
    }

    @Test
    void only_an_offset_past_the_start_has_a_previous_page() {
        assertThat(ScimOffsetPage.of(0, 10, SORT).hasPrevious()).isFalse();
        assertThat(ScimOffsetPage.of(1, 10, SORT).hasPrevious()).isTrue();
        assertThat(ScimOffsetPage.of(10, 10, SORT).hasPrevious()).isTrue();
    }

    @Test
    void the_previous_page_steps_back_by_one_limit() {
        assertThat(ScimOffsetPage.of(25, 10, SORT).previousOrFirst().getOffset()).isEqualTo(15);
    }

    /**
     * From an offset smaller than one page there is no previous page to step back to, so this
     * must answer the FIRST page rather than subtract into a negative offset — which
     * {@link ScimOffsetPage#of} would reject outright, turning a navigation call into an
     * exception.
     */
    @Test
    void stepping_back_from_within_the_first_page_answers_the_first_page() {
        assertThat(ScimOffsetPage.of(4, 10, SORT).previousOrFirst().getOffset()).isZero();
        assertThat(ScimOffsetPage.of(0, 10, SORT).previousOrFirst().getOffset()).isZero();
    }

    @Test
    void the_first_page_starts_at_zero_and_keeps_the_limit() {
        Pageable first = ScimOffsetPage.of(93, 10, SORT).first();

        assertThat(first.getOffset()).isZero();
        assertThat(first.getPageSize()).isEqualTo(10);
    }

    /** A page number is multiplied by the limit, in long arithmetic so a large page cannot wrap. */
    @Test
    void a_page_number_resolves_to_that_page_times_the_limit() {
        assertThat(ScimOffsetPage.of(0, 10, SORT).withPage(0).getOffset()).isZero();
        assertThat(ScimOffsetPage.of(0, 10, SORT).withPage(3).getOffset()).isEqualTo(30);
        assertThat(ScimOffsetPage.of(0, 100, SORT).withPage(30_000_000).getOffset())
                .as("the multiplication is widened to long, so a large page does not wrap negative")
                .isEqualTo(3_000_000_000L);
    }

    @Test
    void a_negative_offset_or_an_empty_page_is_rejected() {
        assertThatThrownBy(() -> ScimOffsetPage.of(-1, 10, SORT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("offset");
        assertThatThrownBy(() -> ScimOffsetPage.of(0, 0, SORT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit");
    }
}
