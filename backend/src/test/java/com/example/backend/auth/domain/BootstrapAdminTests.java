package com.example.backend.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BootstrapAdminTests {

    private static final BootstrapAdmin BOOTSTRAP_ADMIN = new BootstrapAdmin("admin");

    @Test
    void identifiesTheAccountCarryingTheConfiguredRecoveryUsername() {
        Account recovery = new Account("admin", "hash", AccountRole.ADMIN);

        assertThat(BOOTSTRAP_ADMIN.identifies(recovery)).isTrue();
    }

    /**
     * Being an administrator is not what earns the exemption — otherwise every
     * administrator would be unlockable-by-nobody and the lockout would stop
     * applying to the accounts it matters most for.
     */
    @Test
    void identifiesNoOtherAccountEvenAnotherAdministrator() {
        assertThat(BOOTSTRAP_ADMIN.identifies(
                new Account("ada", "hash", AccountRole.ADMIN))).isFalse();
        assertThat(BOOTSTRAP_ADMIN.identifies(
                new Account("bob", "hash", AccountRole.USER))).isFalse();
        assertThat(BOOTSTRAP_ADMIN.identifies(
                new Account("ADMIN", "hash", AccountRole.ADMIN))).isFalse();
    }

    @Test
    void identifiesNothingWhenThereIsNoAccount() {
        assertThat(BOOTSTRAP_ADMIN.identifies(null)).isFalse();
    }

    /**
     * A deployment with no recovery username configured would leave every account
     * lockable and no guaranteed way back in, so this refuses to be constructed
     * rather than silently matching nothing.
     */
    @Test
    void refusesToNameNoAccountAtAll() {
        assertThatThrownBy(() -> new BootstrapAdmin(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The bootstrap admin needs a username");
        assertThatThrownBy(() -> new BootstrapAdmin("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The bootstrap admin needs a username");
    }
}
