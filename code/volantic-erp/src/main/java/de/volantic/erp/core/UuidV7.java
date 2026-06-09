package de.volantic.erp.core;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Erzeugt zeit-sortierte {@link UUID}s nach <strong>UUID Version 7</strong> (RFC 9562).
 *
 * <p>Anwendungsseitig generiert (nicht DB-seitig), damit Schlüssel vor dem Insert feststehen und
 * über alle Module hinweg einheitlich vergeben werden. Die führenden 48 Bit sind ein
 * Millisekunden-Zeitstempel — dadurch sind die IDs grob monoton steigend, was Index-Lokalität gibt
 * und B-Tree-Fragmentierung (anders als bei zufälligem UUIDv4) vermeidet.
 */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7() {
    }

    public static UUID randomUuid() {
        long timestamp = System.currentTimeMillis() & 0xFFFF_FFFF_FFFFL; // 48 Bit unix_ts_ms

        byte[] randA = new byte[2];
        RANDOM.nextBytes(randA);
        long randomA = ((long) (randA[0] & 0xFF) << 8 | (randA[1] & 0xFF)) & 0x0FFF; // 12 Bit

        long msb = (timestamp << 16) | (0x7L << 12) | randomA; // ts | version(7) | rand_a

        byte[] randB = new byte[8];
        RANDOM.nextBytes(randB);
        long lsb = 0L;
        for (byte b : randB) {
            lsb = (lsb << 8) | (b & 0xFF);
        }
        lsb &= 0x3FFF_FFFF_FFFF_FFFFL; // obere 2 Bit löschen …
        lsb |= 0x8000_0000_0000_0000L; // … und Variante (10) setzen

        return new UUID(msb, lsb);
    }
}
