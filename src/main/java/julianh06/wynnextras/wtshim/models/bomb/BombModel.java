// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — BombModel.
 *
 * Listens to chat messages (via WynntilsCompatInit's pipeline) and parses Wynncraft's bomb-bell
 * announcements into BombInfo entries. Exposes the full list of currently-active bombs via
 * getBombBells(). /we bombshare and the bomb overlay read from here.
 */
package julianh06.wynnextras.wtshim.models.bomb;

import julianh06.wynnextras.wtshim.core.components.Model;
import julianh06.wynnextras.wtshim.handlers.chat.event.ChatMessageEvent;
import julianh06.wynnextras.wtshim.models.worlds.type.BombInfo;
import julianh06.wynnextras.wtshim.models.worlds.type.BombType;
import net.neoforged.bus.api.SubscribeEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BombModel extends Model {
    // Source: Wynntils BombModel.java (BOMB_BELL_PATTERN) — with §#RRGGBBAA color codes stripped,
    // since we feed this with MC Text.getString() plain text (no embedded codes).
    private static final Pattern BOMB_BELL = Pattern.compile(
            "^(?:\uE01E\uE002|\uE001) (?<user>.+) has thrown an? (?<bomb>.+) Bomb ( ?)on (?<server>.+)$");

    // Source: Wynntils BOMB_EXPIRED_PATTERN, color-stripped.
    private static final Pattern BOMB_EXPIRED = Pattern.compile(
            "^(?:\uE014\uE002|\uE001) .+ (?<bomb>.+) Bomb has expired!.*$");

    // Source: Wynntils BOMB_THROWN_PATTERN — local same-server announcement.
    private static final Pattern BOMB_LOCAL = Pattern.compile(
            "^(?:\uE014\uE002|\uE001) (?<bomb>.+) Bomb$");

    private final Set<BombInfo> bombs = new LinkedHashSet<>();

    public Set<BombInfo> getBombBells() {
        pruneExpired();
        return Collections.unmodifiableSet(bombs);
    }

    // Wynntils' BombModel subscribes to ChatMessageEvent.Match. Patterns are color-stripped and
    // matched against StyledText#getStringWithoutFormatting (the PUA bell glyphs survive stripping).
    // Deviation: the InfoBar (current-server) TrackedBar path and BombEvent posting are dropped —
    // no WynnExtras caller reads them (only getBombBells()).
    @SubscribeEvent
    public void onChat(ChatMessageEvent.Match event) {
        String plainText = event.getMessage().getStringWithoutFormatting();
        if (plainText == null || plainText.isEmpty()) return;

        Matcher bell = BOMB_BELL.matcher(plainText);
        if (bell.matches()) {
            BombType type = BombType.fromString(bell.group("bomb"));
            if (type != null) {
                add(new BombInfo(
                        bell.group("user").trim(),
                        type,
                        bell.group("server").trim(),
                        System.currentTimeMillis(),
                        type.getActiveMinutes()));
                return;
            }
        }

        Matcher expired = BOMB_EXPIRED.matcher(plainText);
        if (expired.matches()) {
            BombType type = BombType.fromString(expired.group("bomb"));
            if (type != null) {
                removeFirstOfType(type);
                return;
            }
        }

        // Local announcement (own server) — no user/server in line, skip for now.
        // We'd need the next line ("by Player on Server") to associate properly.
    }

    private void add(BombInfo info) {
        pruneExpired();
        // De-dupe by (server, type) — a newer bomb replaces an older one for same server+type.
        List<BombInfo> dupes = new ArrayList<>();
        for (BombInfo b : bombs) {
            if (b.bomb() == info.bomb() && b.server().equalsIgnoreCase(info.server())) dupes.add(b);
        }
        bombs.removeAll(dupes);
        bombs.add(info);
    }

    private void removeFirstOfType(BombType type) {
        for (BombInfo b : bombs) {
            if (b.bomb() == type) { bombs.remove(b); return; }
        }
    }

    private void pruneExpired() {
        bombs.removeIf(b -> !b.isActive());
    }

    public void clear() {
        bombs.clear();
    }
}
