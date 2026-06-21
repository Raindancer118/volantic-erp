package de.volantic.erp.crm.domain.model;

import java.util.regex.Pattern;

/**
 * Shared normalization/validation of e-mail addresses for the CRM aggregates (customer, contact). Kept
 * in one place so the rule does not drift between aggregates.
 *
 * <p>Validation is deliberately a pragmatic syntactic check, not full RFC 5322: a non-empty local part,
 * a single {@code @}, and a domain with at least one dot and a label after it. This rejects the obvious
 * garbage that a bare {@code contains("@")} would let through (e.g. {@code "@"}, {@code "a@"},
 * {@code "a@b"}) without pretending to verify deliverability.
 */
final class EmailAddresses {

    private static final Pattern PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private EmailAddresses() {
    }

    /** Returns the lower-cased, trimmed address, or {@code null} for blank input; rejects malformed input. */
    static String normalize(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String normalized = email.strip().toLowerCase();
        if (!PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("email is not a valid address: " + email);
        }
        return normalized;
    }
}
