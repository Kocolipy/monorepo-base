package com.example.backend.scim.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * A {@link Pageable} of an arbitrary offset and limit, which SCIM needs and
 * {@code PageRequest} cannot express.
 *
 * <p>{@code PageRequest} takes a page NUMBER, so the only offsets it can reach are
 * multiples of the page size. SCIM's {@code startIndex} is a one-based index into the
 * result set and a client may send any value at all — {@code startIndex=7&count=100} is
 * a conformant request, and there is no page number that means it. Paging by page
 * number would either refuse that request or quietly serve a different one.
 *
 * <p>Only {@link #getOffset()}, {@link #getPageSize()} and {@link #getSort()} are
 * load-bearing: those are what Spring Data turns into {@code setFirstResult},
 * {@code setMaxResults} and {@code ORDER BY}. The navigation methods are implemented
 * against the same arithmetic {@code PageRequest} uses so they are consistent rather
 * than correct-by-accident, but nothing in this service calls them — SCIM paging is
 * stateless, and the client computes the next {@code startIndex} itself.
 */
final class ScimOffsetPage implements Pageable {

    private final long offset;

    private final int limit;

    private final Sort sort;

    private ScimOffsetPage(long offset, int limit, Sort sort) {
        this.offset = offset;
        this.limit = limit;
        this.sort = sort;
    }

    /**
     * @param offset zero-based index of the first row, never negative
     * @param limit  how many rows at most, at least one — a page of zero rows is
     *               answered without a query at all, because Spring Data treats a page
     *               size of zero as a programming error rather than as an empty page
     */
    static ScimOffsetPage of(long offset, int limit, Sort sort) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be at least one");
        }
        return new ScimOffsetPage(offset, limit, sort);
    }

    @Override
    public int getPageNumber() {
        return (int) (offset / limit);
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new ScimOffsetPage(offset + limit, limit, sort);
    }

    @Override
    public Pageable previousOrFirst() {
        return hasPrevious() ? new ScimOffsetPage(offset - limit, limit, sort) : first();
    }

    @Override
    public Pageable first() {
        return new ScimOffsetPage(0, limit, sort);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new ScimOffsetPage((long) pageNumber * limit, limit, sort);
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }
}
