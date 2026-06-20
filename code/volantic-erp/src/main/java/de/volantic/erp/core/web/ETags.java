package de.volantic.erp.core.web;

/**
 * Helpers for optimistic-concurrency over HTTP (ETag / If-Match). A resource's optimistic-lock version is
 * exposed as a strong {@code ETag} (a quoted number) on reads and writes; a conditional update must echo
 * it back in {@code If-Match}. Kept in the core (OPEN) module so every module's api layer formats and
 * parses the header the same way. An update endpoint should require {@code If-Match} (a missing header is
 * a 428 Precondition Required) and reject a stale value with 412 (see the modules' exception handlers).
 */
public final class ETags {

    private ETags() {
    }

    /** Formats an optimistic-lock version as a strong ETag, e.g. {@code "7"}. */
    public static String format(long version) {
        return "\"" + version + "\"";
    }

    /** Signals a conditional request that did not supply the required {@code If-Match} header (→ 428). */
    public static final class IfMatchRequiredException extends RuntimeException {
        public IfMatchRequiredException() {
            super("If-Match header with the current ETag is required for updates");
        }
    }

    /**
     * Parses the numeric version out of an {@code If-Match} header value (tolerating the optional
     * {@code W/} weak prefix and surrounding quotes). A missing value is an {@link IfMatchRequiredException}
     * (→ 428); a malformed value is an {@link IllegalArgumentException} (→ 400).
     */
    public static long parseIfMatch(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new IfMatchRequiredException();
        }
        String value = ifMatch.trim();
        if (value.startsWith("W/")) {
            value = value.substring(2);
        }
        value = value.replace("\"", "").trim();
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("malformed If-Match header: " + ifMatch);
        }
    }
}
