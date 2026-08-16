package julianh06.wynnextras.wynncraft.state;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.DisconnectEvent;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.event.WorldChangeEvent;
import julianh06.wynnextras.features.tomes.TomeState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WEModule
public final class CharacterState {
    private static final Pattern CHARACTER_ID = Pattern.compile("^[a-z0-9]{8}$");
    private static final Pattern LEVEL = Pattern.compile("(?i)(?:combat\\s+)?(?:level|lv\\.?)\\D{0,5}(\\d{1,3})");
    private static final Pattern WORLD = Pattern.compile("(?i)\\b(WC\\d+)\\b");

    private static String id;
    private static CharacterClass characterClass = CharacterClass.UNKNOWN;
    private static int level;
    private static String world;
    private static long updatedAt;

    public static Optional<String> id() {
        return Optional.ofNullable(id);
    }

    public static CharacterClass characterClass() {
        return characterClass;
    }

    public static Optional<String> className() {
        return characterClass == CharacterClass.UNKNOWN ? Optional.empty() : Optional.of(characterClass.displayName());
    }

    public static boolean isClass(CharacterClass expected) {
        return expected != null && characterClass == expected;
    }

    public static int level() {
        return level;
    }

    public static Optional<String> world() {
        return Optional.ofNullable(world);
    }

    public static long updatedAt() {
        return updatedAt;
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (event.ticks % 5 != 0) return;
        update();
        SkillPointState.updateFromCurrentMenu();
        TomeState.updateFromCurrentMenu();
    }

    @SubscribeEvent
    public void onWorldChange(WorldChangeEvent event) {
        world = null;
    }

    @SubscribeEvent
    public void onDisconnect(DisconnectEvent event) {
        id = null;
        characterClass = CharacterClass.UNKNOWN;
        level = 0;
        world = null;
        updatedAt = 0;
    }

    public static void update() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.player == null || client.world == null) return;

            String compassId = characterIdFromCompass(client.player.getInventory().getStack(7));
            if (compassId != null) id = compassId;

            Scoreboard scoreboard = client.world.getScoreboard();
            for (ScoreboardObjective objective : scoreboard.getObjectives()) {
                parseLine(objective.getDisplayName().getString());
                for (ScoreboardEntry entry : scoreboard.getScoreboardEntries(objective)) {
                    parseLine(entry.name().getString());
                }
            }
            updatedAt = System.currentTimeMillis();
        } catch (Throwable ignored) {}
    }

    static String characterIdFromCompass(ItemStack compass) {
        if (compass == null || compass.isEmpty()) return null;
        LoreComponent lore = compass.get(DataComponentTypes.LORE);
        if (lore == null) return null;
        for (Text line : lore.lines()) {
            String clean = clean(line.getString());
            if (CHARACTER_ID.matcher(clean).matches()) return clean;
        }
        return null;
    }

    private static void parseLine(String value) {
        String clean = clean(value);
        CharacterClass parsedClass = CharacterClass.parse(clean);
        if (parsedClass != CharacterClass.UNKNOWN) characterClass = parsedClass;
        Matcher levelMatcher = LEVEL.matcher(clean);
        if (levelMatcher.find()) level = safeInt(levelMatcher.group(1), level);
        Matcher worldMatcher = WORLD.matcher(clean);
        if (worldMatcher.find()) world = worldMatcher.group(1).toUpperCase();
    }

    private static String clean(String value) {
        return value == null ? "" : value.replaceAll("§[0-9a-fk-or]", "").trim();
    }

    private static int safeInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
