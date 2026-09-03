package julianh06.wynnextras.wynncraft.state;

import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StatusEffectState {
    public record Effect(String name, String display, int duration) {}
    private static final String TITLE = "Status Effects";
    private static final Pattern EFFECT = Pattern.compile(
            "(?<prefix>.+?)\\s?(?<modifier>(?:-|\\+)?[-.\\d]+)?(?<modifierSuffix>(?:/\\d+s|%)?)?\\s?"
                    + "(?<name>\\+?['a-zA-Z/\\s]+?)\\s\\((?<minutes>\\d{2}|\\*{2}):(?<seconds>\\d{2}|\\*{2})\\)");
    private static final List<Effect> EFFECTS = new ArrayList<>();

    public static List<Effect> effects() { return List.copyOf(EFFECTS); }
    public static boolean hasEffect(String name) {
        return name != null && EFFECTS.stream().anyMatch(effect -> effect.name.equalsIgnoreCase(name)
                || effect.display.toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT)));
    }

    public static void update(Text footer) {
        List<Effect> parsed = parseFooter(footer == null ? null : footer.getString());
        EFFECTS.clear();
        EFFECTS.addAll(parsed);
    }

    public static void clear() {
        EFFECTS.clear();
    }

    static List<Effect> parseFooter(String footer) {
        if (footer == null) return List.of();
        String plain = clean(footer);
        if (!plain.startsWith(TITLE)) return List.of();

        List<Effect> parsed = new ArrayList<>();
        String body = plain.substring(TITLE.length()).strip();
        for (String entry : body.split("\\s{2,}|\\R")) {
            String display = entry.strip();
            if (display.isEmpty()) continue;
            Matcher matcher = EFFECT.matcher(display);
            if (!matcher.find()) continue;

            String name = matcher.group("name").trim();
            int minutes = parseTimePart(matcher.group("minutes"));
            int seconds = parseTimePart(matcher.group("seconds"));
            int duration = minutes < 0 || seconds < 0 ? -1 : minutes * 60 + seconds;
            parsed.add(new Effect(name, display, duration));
        }
        return List.copyOf(parsed);
    }

    private static int parseTimePart(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static String clean(String value) { return value.replaceAll("§[0-9a-fk-or]", "").trim(); }
}
