package com.example.backend.auth.application;

import com.example.backend.audit.domain.AuditRefusalReason;
import com.example.backend.observability.LogEvent;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

/**
 * Logging in: submitted credentials become either an authentication or a
 * refusal, and the attempt is counted against the account either way.
 *
 * <p>The counting is here rather than at the call site because it is part of
 * what logging in <em>is</em> — an entry point that authenticated credentials
 * without it would silently have no lockout. A caller therefore cannot obtain an
 * authentication from submitted credentials and skip the failure run; it gets
 * both or neither.
 *
 * <p>Enforcement is deliberately elsewhere: {@link AccountService} reports a
 * locked account to Spring Security, which refuses it before any password is
 * compared. This module records what happened; the rule for what counts as
 * locked lives in {@link com.example.backend.auth.domain.Account}.
 */
@Service
public class LoginService {

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    /**
     * Value of {@code event.action} on both records this class emits, so a log
     * search finds the accepted and the refused attempt together.
     */
    private static final String LOGIN_ACTION = "login";

    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService attempts;
    private final AccountService accounts;

    public LoginService(
            AuthenticationManager authenticationManager,
            LoginAttemptService attempts,
            AccountService accounts) {
        this.authenticationManager = authenticationManager;
        this.attempts = attempts;
        this.accounts = accounts;
    }

    /**
     * Authenticates the submitted credentials, counting the attempt against the
     * account.
     *
     * <p>A refusal is rethrown unchanged — wrong password, unknown username,
     * locked account, disabled account — so every one of them leaves through the
     * caller's single handler and answers with the same bare {@code 401}.
     *
     * <p>The run of failures an accepted login ends is cleared before this
     * returns, so a caller holding an authentication is by definition one whose
     * account was not refused, whatever it does with the authentication next.
     *
     * <p>Both outcomes are logged, and neither record names the account. The
     * submitted {@code username} is the single most sensitive value passing
     * through here — it is half a credential, and on a failed attempt it is very
     * often a mistyped password — so it stays out of the log, in the message and
     * in the context alike. What the records do carry is the outcome and, for a
     * refusal, the type of refusal, which is what tells a run of wrong passwords
     * from a run against accounts that do not exist. Correlating a record to an
     * account is the audit trail's job, by stable id, once the account aggregate
     * has one.
     *
     * @throws AuthenticationException when the credentials are refused
     */
    public LoginOutcome logIn(String username, String password) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(username, password));
        } catch (AuthenticationException refused) {
            attempts.recordFailure(username, refusalReason(refused));
            // The exception's own type, not its message: a message can carry the
            // submitted value, and a type name is this service's own vocabulary.
            log.atWarn()
                    .addKeyValue(LogEvent.ACTION, LOGIN_ACTION)
                    .addKeyValue(LogEvent.OUTCOME, LogEvent.FAILURE)
                    .addKeyValue(LogEvent.REASON, refused.getClass().getSimpleName())
                    .log("Login refused");
            throw refused;
        }

        // Outside the catch above on purpose: a failure recording the success is
        // not a refusal, and must not be reported to the caller as one.
        attempts.recordSuccess(authentication.getName());
        log.atInfo()
                .addKeyValue(LogEvent.ACTION, LOGIN_ACTION)
                .addKeyValue(LogEvent.OUTCOME, LogEvent.SUCCESS)
                .log("Login accepted");
        return new LoginOutcome(authentication, accounts.resolveAccountId(authentication.getName()));
    }

    /**
     * The refusal as the audit trail's own vocabulary.
     *
     * <p>Translated here, at the one place a Spring Security
     * {@code AuthenticationException} is caught, so no other layer has to know the
     * library's exception hierarchy and no exception object — whose message may
     * name the submitted username — travels further than this method.
     *
     * <p>{@link AuditRefusalReason#UNKNOWN_ACCOUNT} is deliberately not produced
     * here. Spring Security hides a missing account behind
     * {@code BadCredentialsException} so that the two are indistinguishable to the
     * caller, which is the behaviour this service wants; whether the username named
     * an account is settled by {@link LoginAttemptService}, which has to look the
     * account up anyway and can tell without guessing from an exception type.
     */
    private static AuditRefusalReason refusalReason(AuthenticationException refused) {
        return switch (refused) {
            case LockedException locked -> AuditRefusalReason.ACCOUNT_LOCKED;
            case DisabledException disabled -> AuditRefusalReason.ACCOUNT_DISABLED;
            case BadCredentialsException wrong -> AuditRefusalReason.BAD_CREDENTIALS;
            default -> AuditRefusalReason.OTHER;
        };
    }

    /**
     * A successful login, carrying both what Spring Security needs to place in
     * the security context and the account's stable id — the key the web adapter
     * writes into the session index, so application-owned session lookups
     * survive a later username change instead of following
     * {@code authentication.getName()}.
     */
    public record LoginOutcome(Authentication authentication, UUID accountId) {
    }
}
