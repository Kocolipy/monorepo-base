package com.example.backend.auth.application;

import com.example.backend.scim.domain.ScimGroupRepository;
import com.example.backend.scim.domain.ScimUser;
import com.example.backend.scim.domain.ScimUserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The authenticated self-read: a signed-in User's own record, and no one else's.
 *
 * <p>Takes the stable id and nothing else, and the only caller hands it the id the SESSION holds,
 * so there is no parameter a client could fill with somebody else's identifier. It reads and
 * never writes, and it is not the login path's service: {@link LoginIdentityService} answers
 * Spring Security, this answers the owner.
 *
 * <p>Everything is read from the directory at request time rather than from the session's
 * authentication, which names the User by the {@code userName} it had at login. A rename, a
 * membership change or a newly set change-required flag since then is therefore reported as it
 * stands.
 */
@Service
public class SelfReadService {

    private final ScimUserRepository users;
    private final ScimGroupRepository groups;

    public SelfReadService(ScimUserRepository users, ScimGroupRepository groups) {
        this.users = users;
        this.groups = groups;
    }

    /**
     * The record of the User this stable id names.
     *
     * @throws UnknownSessionIdentityException when no live User carries the id — a session that
     *                                         outlived its User identifies nobody
     */
    @Transactional(readOnly = true)
    public SelfRecord read(UUID userId) {
        ScimUser user = users.findById(userId).orElseThrow(UnknownSessionIdentityException::new);
        List<SelfRecord.Group> memberships = groups.findGroupsOfUser(user.id()).stream()
                .map(group -> new SelfRecord.Group(group.id(), group.displayName()))
                .toList();
        return new SelfRecord(
                user.id(),
                user.profile().userName(),
                user.profile().displayName(),
                memberships,
                user.login().isPasswordChangeRequired(),
                user.login().lastAuthenticatedAt());
    }
}
