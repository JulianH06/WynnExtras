package julianh06.wynnextras.features.raid;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RaidParser {
    public LocalDateTime from;
    public LocalDateTime until;
    public List<String> players = new ArrayList<>();

    public static RaidParser parse(String input) {
        RaidParser filter = new RaidParser();
        String[] tokens = input.split("\\s+");

        for (String token : tokens) {
            if (token.startsWith("from:")) {
                filter.from = parseDate(token.substring(5));
            } else if (token.startsWith("until:")) {
                filter.until = parseDate(token.substring(6));
            } else if (token.startsWith("players:")) {
                filter.players = Arrays.asList(token.substring(8).split(","));
            }
        }

        return filter;
    }

    private static LocalDateTime parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        // 1. Relativangabe mit Suffix: -<Zahl><Einheit>
        //    z.B. -1d, -5h, -30m, -10s
        if (raw.startsWith("-")) {
            String rel = raw.substring(1);
            // Suche alle "<Zahl><Einheit>" Paare
            Pattern p = Pattern.compile("(\\d+)([dhms])");
            Matcher m = p.matcher(rel);

            // Wenn kein einziger Treffer, abbrechen
            if (!m.find()) {
                return null;
            }

            // Summe aller Teil-Dauern
            Duration total = Duration.ZERO;
            m.reset();
            while (m.find()) {
                long val = Long.parseLong(m.group(1));
                switch (m.group(2)) {
                    case "d" -> total = total.plusDays(val);
                    case "h" -> total = total.plusHours(val);
                    case "m" -> total = total.plusMinutes(val);
                    case "s" -> total = total.plusSeconds(val);
                }
            }

            // Mindestens 1 Sekunde relativ?
            if (total.isZero()) {
                return null;
            }
            return LocalDateTime.now().minus(total);
        }

        // 2. Absolutes Datum + Zeit (ISO-Format): "yyyy-MM-dd'T'HH:mm[:ss]"
        DateTimeFormatter[] formatters = {
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,                     // 2025-09-02T14:30:00
                DateTimeFormatter.ofPattern("yyyy-MM-dd['/'HH:mm]"),       // 2025-09-02 oder 2025-09-02T14:30
        };

        for (var fmt : formatters) {
            try {
                // Versuche erst LocalDateTime, sonst LocalDate + atStartOfDay()
                return LocalDateTime.parse(raw, fmt);
            } catch (DateTimeParseException e1) {
                // Falls reines Datum erwartet wird
                try {
                    LocalDate date = LocalDate.parse(raw, fmt);
                    return date.atStartOfDay();
                } catch (DateTimeParseException e2) {
                    // nächster Formatter
                }
            }
        }

        // 3. Keine gültige Angabe
        return null;
    }
}
