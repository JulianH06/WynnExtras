// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — StatusEffectModel.
 *
 * Reads Wynncraft's tab-list footer each tick (via PlayerListHudAccessor), parses the
 * "Status Effects" section into StatusEffect records. Revives rebound timer, radiant HUD,
 * and any other feature that branches on effect presence.
 *
 * Wynncraft encodes effects in the footer as space-separated entries, each ending with a
 * "(MM:SS)" timer. The section starts with "Status Effects" — entries follow.
 */
package julianh06.wynnextras.wtshim.models.statuseffects;

import julianh06.wynnextras.wtshim.core.components.Model;
import julianh06.wynnextras.wtshim.core.text.StyledText;
import julianh06.wynnextras.wtshim.fabric.mixin.PlayerListHudAccessor;
import julianh06.wynnextras.wtshim.models.statuseffects.type.StatusEffect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class StatusEffectModel extends Model {
    // Source: Wynntils StatusEffectModel#STATUS_EFFECT_PATTERN — § codes stripped, groups preserved.
    // Original: "(?<prefix>.+?)§7\\s?(?<modifier>(\\-|\\+)?([\\-\\.\\d]+))?(?<modifierSuffix>((\\/\\d+s)|%)?)?\\s?(?<name>\\+?['a-zA-Z\\/\\s]+?)\\s(?<timer>§[84a]\\((?<minutes>(\\d{2}|\\*{2})):(?<seconds>(\\d{2}|\\*{2}))\\))"
    private static final Pattern EFFECT_PATTERN = Pattern.compile(
            "(?<prefix>.+?)\\s?(?<modifier>(?:-|\\+)?[\\-\\.\\d]+)?(?<modifierSuffix>(?:\\/\\d+s|%)?)?\\s?(?<name>\\+?['a-zA-Z\\/\\s]+?)\\s\\((?<minutes>\\d{2}|\\*{2}):(?<seconds>\\d{2}|\\*{2})\\)");

    // Source: Wynntils STATUS_EFFECTS_TITLE — "§d§lStatus Effects" → color-stripped "Status Effects".
    private static final String HEADER_MARKER = "Status Effects";

    private List<StatusEffect> statusEffects = List.of();

    public StatusEffectModel() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> tick());
    }

    public List<StatusEffect> getStatusEffects() { return statusEffects; }

    private void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.inGameHud == null) return;

        PlayerListHud hud = mc.inGameHud.getPlayerListHud();
        if (!(hud instanceof PlayerListHudAccessor accessor)) return;
        Text footer = accessor.getFooter();

        if (footer == null) {
            if (!statusEffects.isEmpty()) statusEffects = List.of();
            return;
        }

        String raw = asRawString(footer);
        String plain = stripColors(raw);
        if (!plain.contains(HEADER_MARKER)) {
            if (!statusEffects.isEmpty()) statusEffects = List.of();
            return;
        }

        // Take everything after the Status Effects marker.
        int idx = plain.indexOf(HEADER_MARKER);
        String body = plain.substring(idx + HEADER_MARKER.length()).strip();
        if (body.isEmpty()) {
            if (!statusEffects.isEmpty()) statusEffects = List.of();
            return;
        }

        // Entries are separated by double-spaces or newlines.
        String[] entries = body.split("\\s{2,}|\\n");
        List<StatusEffect> parsed = new ArrayList<>();
        for (String entry : entries) {
            String trimmed = entry.strip();
            if (trimmed.isEmpty()) continue;
            Matcher m = EFFECT_PATTERN.matcher(trimmed);
            if (!m.find()) continue;

            String name = m.group("name").trim();
            int minutes = parseOr(m.group("minutes"), -1);
            int seconds = parseOr(m.group("seconds"), -1);
            int totalSeconds = (minutes < 0 || seconds < 0) ? -1 : minutes * 60 + seconds;
            parsed.add(new StatusEffect(
                    StyledText.fromString(name),
                    StyledText.fromString(name),
                    totalSeconds));
        }

        this.statusEffects = Collections.unmodifiableList(parsed);
    }

    private static int parseOr(String s, int fallback) {
        try { return Integer.parseInt(s); } catch (Exception e) { return fallback; }
    }

    private static String asRawString(Text text) {
        StringBuilder sb = new StringBuilder();
        text.visit((style, s) -> { sb.append(s); return Optional.empty(); }, Style.EMPTY);
        return sb.toString();
    }

    private static String stripColors(String s) {
        return s == null ? "" : s.replaceAll("§[0-9a-fk-or]", "");
    }
}
