package com.example.backend.auth.controller;

import com.example.backend.scim.domain.ScimAttributeLimits;
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
 * A submitted {@code username} no longer than any stored {@code userName} can be:
 * {@link ScimAttributeLimits#USER_NAME} code points, the SCIM column's own bound and counted the
 * way that bound is.
 *
 * <p>Checked by bean validation, so an over-length name is refused with the same bodiless
 * {@code 400} as any other malformed body, before the request reaches the login service — it is
 * not counted toward a failure run, not audited, and not distinguishable from a blank name.
 * {@code null} is left to {@code @NotBlank}.
 */
@Documented
@Constraint(validatedBy = MaxUserNameLength.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxUserNameLength {

    String message() default "must be at most " + ScimAttributeLimits.USER_NAME + " characters";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** Counts code points, as {@link ScimAttributeLimits} does. */
    final class Validator implements ConstraintValidator<MaxUserNameLength, String> {

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null
                    || value.codePointCount(0, value.length()) <= ScimAttributeLimits.USER_NAME;
        }
    }
}
