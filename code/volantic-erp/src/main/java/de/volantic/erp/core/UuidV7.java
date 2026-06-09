package de.volantic.erp.core;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Generates time-sorted {@link UUID}s following <strong>UUID Version 7</strong> (RFC 9562).
 *
 * <p>Generated on the application side (not by the database) so that keys are fixed before insert and
 * are assigned uniformly across all modules. The leading 48 bits are a millisecond timestamp — this
 * makes the ids roughly monotonically increasing, which gives index locality and avoids B-tree
 * fragmentation (unlike random UUIDv4).
 */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7() {
    }

    public static UUID randomUuid() {
        long timestamp = System.currentTimeMillis() & 0xFFFF_FFFF_FFFFL; // 48 bit unix_ts_ms

        byte[] randA = new byte[2];
        RANDOM.nextBytes(randA);
        long randomA = ((long) (randA[0] & 0xFF) << 8 | (randA[1] & 0xFF)) & 0x0FFF; // 12 bit

        long msb = (timestamp << 16) | (0x7L << 12) | randomA; // ts | version(7) | rand_a

        byte[] randB = new byte[8];
        RANDOM.nextBytes(randB);
        long lsb = 0L;
        for (byte b : randB) {
            lsb = (lsb << 8) | (b & 0xFF);
        }
        lsb &= 0x3FFF_FFFF_FFFF_FFFFL; // clear the top 2 bits …
        lsb |= 0x8000_0000_0000_0000L; // … and set the variant (10)

        return new UUID(msb, lsb);
    }
}
