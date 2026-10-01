package com.example.backend.scim.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.scim.ScimIdentities;
import com.example.backend.scim.domain.ScimEmail;
import com.example.backend.scim.domain.ScimName;
import com.example.backend.scim.domain.ScimUserProfile;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The PUT command carries a plaintext password, so its string form must not. */
class ScimUserReplacementTests {

    @Test
    void the_string_form_says_whether_a_password_was_sent_and_never_what_it_was() {
        ScimUserReplacement withPassword = new ScimUserReplacement(
                ScimIdentities.profile("ada", true), "hunter2-hunter2", null, true);
        ScimUserReplacement withoutPassword = new ScimUserReplacement(
                ScimIdentities.profile("ada", true), null, null, true);

        assertThat(withPassword.toString())
                .doesNotContain("hunter2")
                .endsWith(", password=present]")
                .startsWith("ScimUserReplacement[profile=ScimUserProfile[userName=ada");
        assertThat(withoutPassword.toString()).endsWith(", password=absent]");
    }

    @Test
    void an_unasserted_active_keeps_the_stored_value_and_everything_else_is_replaced() {
        // Every field set to a non-default value, so a field dropped on the way through cannot
        // pass by coinciding with the default the profile's constructor substitutes.
        ScimUserProfile replacement = new ScimUserProfile("ada-renamed",
                new ScimName("Ada Lovelace", "Lovelace", "Ada", null, null, null), "Ada",
                "en", "en-GB", "Europe/London", true, List.of(new ScimEmail("a@x", "work", true)));
        ScimUserReplacement unasserted = new ScimUserReplacement(replacement, null, null, false);

        ScimUserProfile overInactive = unasserted.profileOver(ScimIdentities.profile("ada", false));
        assertThat(overInactive.active()).as("a deactivated User stays deactivated").isFalse();
        assertThat(overInactive).usingRecursiveComparison().ignoringFields("active")
                .isEqualTo(replacement);
        ScimUserProfile inactiveReplacement = new ScimUserProfile("ada", ScimName.NONE, null,
                null, null, null, false, List.of());
        assertThat(new ScimUserReplacement(inactiveReplacement, null, null, false)
                .profileOver(ScimIdentities.profile("ada", true)).active())
                .as("an active User stays active").isTrue();
    }

    @Test
    void an_asserted_active_replaces_the_stored_value() {
        ScimUserProfile reactivate = ScimIdentities.profile("ada", true);
        ScimUserReplacement asserted = new ScimUserReplacement(reactivate, null, null, true);

        assertThat(asserted.profileOver(ScimIdentities.profile("ada", false)))
                .isSameAs(reactivate);
    }
}
