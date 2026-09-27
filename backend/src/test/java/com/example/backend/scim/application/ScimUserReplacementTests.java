package com.example.backend.scim.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.scim.ScimIdentities;
import org.junit.jupiter.api.Test;

/** The PUT command carries a plaintext password, so its string form must not. */
class ScimUserReplacementTests {

    @Test
    void the_string_form_says_whether_a_password_was_sent_and_never_what_it_was() {
        ScimUserReplacement withPassword = new ScimUserReplacement(
                ScimIdentities.profile("ada", true), "hunter2-hunter2", null);
        ScimUserReplacement withoutPassword = new ScimUserReplacement(
                ScimIdentities.profile("ada", true), null, null);

        assertThat(withPassword.toString())
                .doesNotContain("hunter2")
                .endsWith(", password=present]")
                .startsWith("ScimUserReplacement[profile=ScimUserProfile[userName=ada");
        assertThat(withoutPassword.toString()).endsWith(", password=absent]");
    }
}
