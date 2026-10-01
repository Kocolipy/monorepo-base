package com.example.backend.auth.controller;

import com.example.backend.scim.domain.PasswordNormalization;
import com.example.backend.scim.domain.PasswordPolicy;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A submitted password no longer than the password policy lets any password be:
 * {@link PasswordPolicy#MAX_LENGTH} code points of the normalized form, which is exactly how the
 * policy measures it. Measuring the same form is what keeps the two bounds from disagreeing: every
 * password the policy ever accepted can still be submitted here, and nothing longer reaches the
 * login service, the failure run or Argon2id.
 *
 * <p>Checked by bean validation, so an over-length value is refused with the same bodiless
 * {@code 400} as any other malformed body, before any business logic runs: it is not counted
 * toward a failure run and not audited. Normalizing a value to measure it is linear in its length,
 * a small fraction of what a single Argon2id verification of it would cost. {@code null} is left
 * to {@code @NotBlank}.
 */
@Documented
@Constraint(validatedBy = MaxPasswordLength.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxPasswordLength {

    String message() default "must be at most " + PasswordPolicy.MAX_LENGTH + " characters";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** Counts code points of the normalized form, as {@link PasswordPolicy} does. */
    final class Validator implements ConstraintValidator<MaxPasswordLength, String> {

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) {
                return true;
            }
            String normalized = PasswordNormalization.normalize(value);
            return normalized.codePointCount(0, normalized.length()) <= PasswordPolicy.MAX_LENGTH;
        }
    }
}
