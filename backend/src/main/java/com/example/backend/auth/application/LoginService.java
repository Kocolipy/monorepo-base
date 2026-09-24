package com.example.backend.auth.application;

import org.springframework.security.authentication.AuthenticationManager;
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

    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService attempts;

    public LoginService(AuthenticationManager authenticationManager, LoginAttemptService attempts) {
        this.authenticationManager = authenticationManager;
        this.attempts = attempts;
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
     * @throws AuthenticationException when the credentials are refused
     */
    public Authentication logIn(String username, String password) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(username, password));
        } catch (AuthenticationException refused) {
            attempts.recordFailure(username);
            throw refused;
        }

        // Outside the catch above on purpose: a failure recording the success is
        // not a refusal, and must not be reported to the caller as one.
        attempts.recordSuccess(authentication.getName());
        return authentication;
    }
}
