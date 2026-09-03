package julianh06.wynnextras.features.privacy;

import julianh06.wynnextras.core.WynnExtras;
import net.fabricmc.loader.api.FabricLoader;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;

/** Creates an installation-local telemetry identifier which rotates every 30 UTC days. */
public final class AnonymousTelemetryIdentity {
    static final long PERIOD_LENGTH_DAYS = 30;
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Path SEED_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("wynnextras")
            .resolve("anonymous-telemetry-seed");

    private static byte[] seed;

    private AnonymousTelemetryIdentity() {}

    public static AnonymousIdentity current() {
        long epochDay = LocalDate.now(ZoneOffset.UTC).toEpochDay();
        long period = Math.floorDiv(epochDay, PERIOD_LENGTH_DAYS);
        return forPeriod(period);
    }

    static AnonymousIdentity forPeriod(long period) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(seed(), HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(("wynnextras-anonymous:" + period).getBytes(StandardCharsets.UTF_8));
            return new AnonymousIdentity(HexFormat.of().formatHex(digest), period);
        } catch (Exception e) {
            throw new IllegalStateException("Could not create anonymous telemetry identifier", e);
        }
    }

    private static synchronized byte[] seed() {
        if (seed != null) return seed;

        try {
            if (Files.exists(SEED_PATH)) {
                byte[] loaded = Base64.getDecoder().decode(Files.readString(SEED_PATH).trim());
                if (loaded.length == 32) {
                    seed = loaded;
                    return seed;
                }
                WynnExtras.LOGGER.warn("[WynnExtras] Invalid anonymous telemetry seed; replacing it");
            }
        } catch (Exception e) {
            WynnExtras.LOGGER.warn("[WynnExtras] Could not read anonymous telemetry seed: {}", e.getMessage());
        }

        seed = new byte[32];
        new SecureRandom().nextBytes(seed);
        try {
            Files.createDirectories(SEED_PATH.getParent());
            Files.writeString(SEED_PATH, Base64.getEncoder().encodeToString(seed));
        } catch (IOException e) {
            WynnExtras.LOGGER.warn("[WynnExtras] Could not persist anonymous telemetry seed: {}", e.getMessage());
        }
        return seed;
    }

    public record AnonymousIdentity(String id, long period) {}
}
