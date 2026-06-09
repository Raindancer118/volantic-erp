package de.volantic.erp.audit.application;

/**
 * Outcome of an audit-chain integrity check. {@code intact} is false if any entry's recomputed hash
 * does not match its stored hash; {@code brokenAtSequence} then points at the first tampered entry.
 */
public record IntegrityResult(boolean intact, long entriesChecked, Long brokenAtSequence) {

    public static IntegrityResult ok(long entriesChecked) {
        return new IntegrityResult(true, entriesChecked, null);
    }

    public static IntegrityResult broken(long entriesChecked, long brokenAtSequence) {
        return new IntegrityResult(false, entriesChecked, brokenAtSequence);
    }
}
