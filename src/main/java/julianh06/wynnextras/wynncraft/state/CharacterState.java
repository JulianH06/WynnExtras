package julianh06.wynnextras.wynncraft.state;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.DisconnectEvent;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.event.WorldChangeEvent;
import julianh06.wynnextras.features.tomes.TomeState;
import julianh06.wynnextras.wynncraft.menu.MenuType;
import julianh06.wynnextras.wynncraft.menu.WynncraftMenuService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WEModule
public final class CharacterState {
    private static final Pattern CHARACTER_ID = Pattern.compile("^[a-z0-9]{8}$");
    private static final Pattern LEVEL = Pattern.compile("(?i)(?:combat\\s+)?(?:level|lv\\.?)\\D{0,5}(\\d{1,3})");
    private static final Pattern WORLD = Pattern.compile("(?i)\\b((?:NA|EU|AS)\\d+)\\b");
    private static final UUID WORLD_NAME_UUID = UUID.fromString("16ff7452-714f-2752-b3cd-c3cb2068f6af");

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
        update();
        if (event.ticks % 5 != 0) return;
        SkillPointState.updateFromCurrentMenu();
        TomeState.updateFromCurrentMenu();
    }

    @SubscribeEvent
    public void onWorldChange(WorldChangeEvent event) {
        id = null;
        characterClass = CharacterClass.UNKNOWN;
        level = 0;
        world = null;
        updatedAt = 0;
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
            if (compassId != null && !compassId.equals(id)) {
                id = compassId;
                characterClass = CharacterClass.UNKNOWN;
                level = 0;
            }

            updateFromCurrentMenu(client);

            String playerListWorld = worldFromPlayerList(client);
            if (playerListWorld != null) world = playerListWorld;
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

    private static String worldFromPlayerList(MinecraftClient client) {
        if (client.getNetworkHandler() == null) return null;
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(WORLD_NAME_UUID);
        if (entry == null || entry.getDisplayName() == null) return null;
        return worldFromText(entry.getDisplayName().getString());
    }

    static String worldFromText(String value) {
        Matcher matcher = WORLD.matcher(clean(value));
        return matcher.find() ? matcher.group(1).toUpperCase(Locale.ROOT) : null;
    }

    static void updateCharacterInfo(String className, int combatLevel) {
        CharacterClass parsedClass = CharacterClass.parse(className);
        if (parsedClass != CharacterClass.UNKNOWN) characterClass = parsedClass;
        if (combatLevel > 0) level = combatLevel;
        if (parsedClass != CharacterClass.UNKNOWN || combatLevel > 0) updatedAt = System.currentTimeMillis();
    }

    private static void updateFromCurrentMenu(MinecraftClient client) {
        if (!WynncraftMenuService.isCurrent(MenuType.CHARACTER_INFO)) return;
        if (!(client.currentScreen instanceof HandledScreen<?> screen)) return;
        if (screen.getScreenHandler().slots.size() <= 7) return;

        ItemStack characterInfo = screen.getScreenHandler().slots.get(7).getStack();
        if (characterInfo == null || characterInfo.isEmpty()) return;
        CharacterClass parsedClass = CharacterClass.UNKNOWN;
        int parsedLevel = 0;

        CharacterInfo lineInfo = parseCharacterInfo(characterInfo.getName().getString());
        if (lineInfo.characterClass() != CharacterClass.UNKNOWN) parsedClass = lineInfo.characterClass();
        if (lineInfo.level() > 0) parsedLevel = lineInfo.level();
        LoreComponent lore = characterInfo.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) {
                lineInfo = parseCharacterInfo(line.getString());
                if (lineInfo.characterClass() != CharacterClass.UNKNOWN) parsedClass = lineInfo.characterClass();
                if (lineInfo.level() > 0) parsedLevel = lineInfo.level();
            }
        }

        if (parsedClass != CharacterClass.UNKNOWN) characterClass = parsedClass;
        if (parsedLevel > 0) level = parsedLevel;
        if (parsedClass != CharacterClass.UNKNOWN && parsedLevel > 0 && id != null) {
            CharacterStateIntegration.savePassiveCharacterInfo(id, parsedClass.displayName(), parsedLevel);
        }
    }

    private static CharacterInfo parseCharacterInfo(String value) {
        String clean = clean(value);
        CharacterClass parsedClass = CharacterClass.parse(clean);
        Matcher levelMatcher = LEVEL.matcher(clean);
        int parsedLevel = levelMatcher.find() ? safeInt(levelMatcher.group(1), 0) : 0;
        return new CharacterInfo(parsedClass, parsedLevel);
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

    private record CharacterInfo(CharacterClass characterClass, int level) {}
}
