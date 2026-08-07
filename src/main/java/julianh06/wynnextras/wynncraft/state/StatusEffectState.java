package julianh06.wynnextras.wynncraft.state;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.utils.BossBarUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WEModule
public final class StatusEffectState {
    public record Effect(String name, String display, int duration) {}
    private static final Pattern SECONDS = Pattern.compile("(?i)(\\d+)\\s*s(?:ec(?:onds?)?)?\\b");
    private static final Pattern CLOCK = Pattern.compile("\\b(\\d+):(\\d{2})\\b");
    private static final List<Effect> EFFECTS = new ArrayList<>();

    public static List<Effect> effects() { return List.copyOf(EFFECTS); }
    public static boolean hasEffect(String name) {
        return name != null && EFFECTS.stream().anyMatch(effect -> effect.name.equalsIgnoreCase(name)
                || effect.display.toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT)));
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (event.ticks % 5 != 0) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        List<Effect> parsed = new ArrayList<>();
        for (ClientBossBar bar : BossBarUtils.getBossBars(client.inGameHud.getBossBarHud())) {
            String display = clean(bar.getName().getString());
            if (display.isEmpty() || display.contains("Tower") || display.toLowerCase(Locale.ROOT).contains("raid")) continue;
            int duration = duration(display);
            if (duration < 0 && !looksLikeEffect(display)) continue;
            parsed.add(new Effect(effectName(display), display, duration));
        }
        EFFECTS.clear();
        EFFECTS.addAll(parsed);
    }

    private static boolean looksLikeEffect(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("radiant") || lower.contains("radiance") || lower.contains("provoke")
                || lower.contains("tree manipulation");
    }

    private static int duration(String text) {
        Matcher seconds = SECONDS.matcher(text);
        if (seconds.find()) return Integer.parseInt(seconds.group(1));
        Matcher clock = CLOCK.matcher(text);
        if (clock.find()) return Integer.parseInt(clock.group(1)) * 60 + Integer.parseInt(clock.group(2));
        return -1;
    }

    private static String effectName(String display) {
        return display.replaceAll("(?i)\\s*[-|:]?\\s*(?:\\d+\\s*s(?:ec(?:onds?)?)?|\\d+:\\d{2}).*", "").trim();
    }

    private static String clean(String value) { return value.replaceAll("§[0-9a-fk-or]", "").trim(); }
}
