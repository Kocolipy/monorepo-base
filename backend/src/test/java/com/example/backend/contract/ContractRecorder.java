package com.example.backend.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Performs exchanges and holds each one against {@link OpenApiContract} as it returns.
 *
 * <p>Every exchange a contract suite makes goes through {@link #perform}, so there is no request
 * whose response is exempt from the check: a fixture that asserts a {@code 200} and nothing else
 * still fails when that {@code 200} carries a body the document does not describe. What each
 * exchange covered is collected, so the suite can then ask the reverse question — whether every
 * status the document promises was actually produced by some fixture.
 */
public final class ContractRecorder {

    private final OpenApiContract contract;

    private final Set<String> covered = Collections.synchronizedSet(new LinkedHashSet<>());

    public ContractRecorder(OpenApiContract contract) {
        this.contract = contract;
    }

    /** Performs the request, fails on any departure from the contract, and records coverage. */
    public MvcResult perform(MockMvc mvc, MockHttpServletRequestBuilder request) throws Exception {
        MvcResult result = mvc.perform(request).andReturn();
        OpenApiContract.Verdict verdict = contract.check(
                result.getRequest().getMethod(),
                result.getRequest().getRequestURI(),
                result.getResponse());
        assertThat(verdict.violations())
                .as("the exchange departs from docs/openapi.yaml")
                .isEmpty();
        verdict.covered().ifPresent(covered::add);
        return result;
    }

    /** Every documented status some exchange has produced so far. */
    public Set<String> covered() {
        synchronized (covered) {
            return Set.copyOf(covered);
        }
    }

    /**
     * The documented statuses under {@code prefix} that no exchange has produced: an operation's
     * own statuses, plus the namespace-level ones whose namespace starts with it.
     */
    public List<String> uncovered(String prefix) {
        Set<String> expected = new LinkedHashSet<>();
        for (OpenApiContract.Operation operation : contract.operations()) {
            if (operation.template().startsWith(prefix)) {
                for (int status : operation.statuses()) {
                    expected.add(OpenApiContract.operationKey(operation, status));
                }
            }
        }
        for (String namespace : contract.namespaceStatuses()) {
            if (namespace.startsWith("ANY " + prefix)) {
                expected.add(namespace);
            }
        }
        Set<String> seen = covered();
        return expected.stream().filter(key -> !seen.contains(key)).toList();
    }
}
