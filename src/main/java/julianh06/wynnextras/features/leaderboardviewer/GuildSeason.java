package julianh06.wynnextras.features.leaderboardviewer;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public record GuildSeason(
        int number,
        Instant startDate,
        Instant endDate,
        List<Reward> ratingRewards,
        List<Reward> leaderboardRewards
) {
    public GuildSeason {
        ratingRewards = ratingRewards.stream()
                .sorted(Comparator.comparingInt(Reward::threshold))
                .toList();
        leaderboardRewards = leaderboardRewards.stream()
                .sorted(Comparator.comparingInt(Reward::threshold).reversed())
                .toList();
    }

    public boolean isActive() {
        Instant now = Instant.now();
        return !now.isBefore(startDate) && now.isBefore(endDate);
    }

    public String dateRange() {
        return formatDate(startDate) + " - " + formatDate(endDate);
    }

    public static String formatDate(Instant date) {
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
                .withLocale(systemDateLocale())
                .withZone(ZoneId.systemDefault())
                .format(date);
    }

    private static Locale systemDateLocale() {
        String localeName = System.getenv("LC_TIME");
        if (localeName == null || localeName.isBlank() || localeName.equals("C") || localeName.startsWith("C.")) {
            localeName = System.getenv("LANG");
        }
        if (localeName == null || localeName.isBlank() || localeName.equals("C") || localeName.startsWith("C.")) {
            return Locale.getDefault();
        }
        int encodingSeparator = localeName.indexOf('.');
        if (encodingSeparator >= 0) localeName = localeName.substring(0, encodingSeparator);
        int modifierSeparator = localeName.indexOf('@');
        if (modifierSeparator >= 0) localeName = localeName.substring(0, modifierSeparator);
        Locale locale = Locale.forLanguageTag(localeName.replace('_', '-'));
        return locale.getLanguage().isBlank() ? Locale.getDefault() : locale;
    }

    public String status() {
        Instant now = Instant.now();
        if (now.isBefore(startDate)) return "Starts in " + formatDuration(Duration.between(now, startDate));
        if (now.isBefore(endDate)) return "Ends in " + formatDuration(Duration.between(now, endDate));
        return "Ended";
    }

    public Reward lastReachedRatingReward(double score) {
        Reward reached = null;
        for (Reward reward : ratingRewards) {
            if (score < reward.threshold()) break;
            reached = reward;
        }
        return reached;
    }

    public Reward nextRatingReward(double score) {
        for (Reward reward : ratingRewards) {
            if (score < reward.threshold()) return reward;
        }
        return null;
    }

    private static String formatDuration(Duration duration) {
        long seconds = Math.max(0, duration.getSeconds());
        if (seconds < 60 * 60) {
            return seconds / 60 + "m " + seconds % 60 + "s";
        }
        if (seconds < 24 * 60 * 60) {
            return seconds / (60 * 60) + "h " + seconds / 60 % 60 + "m";
        }
        long days = seconds / (24 * 60 * 60);
        if (days < 7) {
            return days + "d " + seconds / (60 * 60) % 24 + "h";
        }
        return days + "d";
    }

    public record Reward(String type, String value, Instant expires, String conditionType, int threshold) {
    }
}
