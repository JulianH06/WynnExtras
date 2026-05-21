package julianh06.wynnextras.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.CompletableFuture;

public class ResetTimeConfig {
    public static final ResetTimeConfig INSTANCE = new ResetTimeConfig();

    // Fallback defaults
    private int lootpoolHour;
    private int lootpoolMinute;
    private String lootpoolDay;
    private String lootpoolTimezone;

    private int lootrunHour;
    private int lootrunMinute;
    private String lootrunDay;
    private String lootrunTimezone;

    private int gambitHour;
    private int gambitMinute;
    private String gambitTimezone;

    private volatile boolean fetched = false;
    private volatile boolean fetching = false;

    public ResetTimeConfig() {
        resetToFallbackDefaults();
    }

    public void fetchIfNeeded() {
        if (fetched || fetching) return;
        fetching = true;

        CompletableFuture.supplyAsync(() -> {
            try {
                String url = "https://wynnextras.com/api/reset-times";

                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(8))
                        .GET()
                        .build();

                return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .handle((response, ex) -> {
                        if (ex != null || response == null) {
                            WynnExtras.LOGGER.error("[WynnExtras] Failed to fetch reset times: " + (ex != null ? ex.getMessage() : "null response"));
                            fetching = false;
                            return null;
                        }
                        if (response.statusCode() != 200) {
                            WynnExtras.LOGGER.error("[WynnExtras] Failed to fetch reset times, Invalid status: " + response.statusCode());
                            fetching = false;
                            return null;
                        }

                        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

                        JsonObject lp = json.getAsJsonObject("lootpool_reset");
                        lootpoolHour = lp.get("hour").getAsInt();
                        lootpoolMinute = lp.get("minute").getAsInt();
                        lootpoolDay = lp.get("day").getAsString();
                        lootpoolTimezone = lp.get("timezone").getAsString();

                        JsonObject lr = json.getAsJsonObject("lootrun_reset");
                        lootrunHour = lr.get("hour").getAsInt();
                        lootrunMinute = lr.get("minute").getAsInt();
                        lootrunDay = lr.get("day").getAsString();
                        lootrunTimezone = lr.get("timezone").getAsString();

                        JsonObject g = json.getAsJsonObject("gambit_reset");
                        gambitHour = g.get("hour").getAsInt();
                        gambitMinute = g.get("minute").getAsInt();
                        gambitTimezone = g.get("timezone").getAsString();

                        fetched = true;
                        fetching = false;
                        WynnExtras.LOGGER.info("[WynnExtras] Successfully fetched reset times");
                        return null;
                    });
            } catch (Exception e) {
                WynnExtras.LOGGER.error("[WynnExtras] Failed to fetch reset times, using defaults: " + e.getMessage());
            }
            fetching = false;
            return null;
        });
    }

    private void resetToFallbackDefaults() {
        lootpoolHour = 17;
        lootpoolMinute = 0;
        lootpoolDay = "FRIDAY";
        lootpoolTimezone = "UTC";

        lootrunHour = 18;
        lootrunMinute = 0;
        lootrunDay = "FRIDAY";
        lootrunTimezone = "UTC";

        gambitHour = 17;
        gambitMinute = 0;
        gambitTimezone = "UTC";
    }

    public void refetch() {
        resetToFallbackDefaults();

        fetched = false;
        fetching = false;

        fetchIfNeeded();
    }

    public ZonedDateTime getCurrentLootpoolReset() {
        ZoneId zone = ZoneId.of(lootpoolTimezone);
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime thisFriday = now
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.valueOf(lootpoolDay)))
                .withHour(lootpoolHour).withMinute(lootpoolMinute).withSecond(0).withNano(0);
        if (now.isBefore(thisFriday)) thisFriday = thisFriday.minusWeeks(1);
        return thisFriday;
    }

    public ZonedDateTime getNextLootpoolReset() {
        ZoneId zone = ZoneId.of(lootpoolTimezone);
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime next = now
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.valueOf(lootpoolDay)))
                .withHour(lootpoolHour).withMinute(lootpoolMinute).withSecond(0).withNano(0);
        if (!next.isAfter(now)) next = next.plusWeeks(1);
        return next;
    }

    public ZonedDateTime getCurrentLootrunReset() {
        ZoneId zone = ZoneId.of(lootrunTimezone);
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime thisFriday = now
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.valueOf(lootrunDay)))
                .withHour(lootrunHour).withMinute(lootrunMinute).withSecond(0).withNano(0);
        if (now.isBefore(thisFriday)) thisFriday = thisFriday.minusWeeks(1);
        return thisFriday;
    }

    public ZonedDateTime getNextLootrunReset() {
        ZoneId zone = ZoneId.of(lootrunTimezone);
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime next = now
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.valueOf(lootrunDay)))
                .withHour(lootrunHour).withMinute(lootrunMinute).withSecond(0).withNano(0);
        if (!next.isAfter(now)) next = next.plusWeeks(1);
        return next;
    }

    public ZonedDateTime getCurrentGambitReset() {
        ZoneId zone = ZoneId.of(gambitTimezone);
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime todayReset = now.withHour(gambitHour).withMinute(gambitMinute).withSecond(0).withNano(0);
        if (now.isBefore(todayReset)) todayReset = todayReset.minusDays(1);
        return todayReset;
    }

    public ZonedDateTime getNextGambitReset() {
        ZoneId zone = ZoneId.of(gambitTimezone);
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime next = now.withHour(gambitHour).withMinute(gambitMinute).withSecond(0).withNano(0);
        if (!next.isAfter(now)) next = next.plusDays(1);
        return next;
    }

    public int getLootpoolHour() { return lootpoolHour; }
    public int getLootpoolMinute() { return lootpoolMinute; }
    public String getLootpoolTimezone() { return lootpoolTimezone; }
    public int getGambitHour() { return gambitHour; }
    public int getGambitMinute() { return gambitMinute; }
    public String getGambitTimezone() { return gambitTimezone; }
    public int getLootrunHour() { return lootrunHour; }
    public int getLootrunMinute() { return lootrunMinute; }
    public String getLootrunTimezone() { return lootrunTimezone; }
}