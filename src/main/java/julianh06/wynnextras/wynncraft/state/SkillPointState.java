package julianh06.wynnextras.wynncraft.state;

import julianh06.wynnextras.wynncraft.menu.MenuType;
import julianh06.wynnextras.wynncraft.menu.WynncraftMenuService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SkillPointState {
    private static final Pattern NUMBER = Pattern.compile("(?:^|\\D)(\\d{1,3})(?:\\D|$)");
    private static final Pattern AVAILABLE = Pattern.compile("(?i)(?:available|remaining|unassigned|unused)\\s+(?:skill\\s*)?points?\\D*(\\d{1,3})");
    private static final Map<SkillPoint, Integer> ASSIGNED = new EnumMap<>(SkillPoint.class);
    private static int available;
    private static boolean availableKnown;
    private static String sourceCharacterId;
    private static long updatedAt;

    private SkillPointState() {}

    public static int assigned(SkillPoint skill) {
        return skill == null || !isCurrentCharacter() ? 0 : ASSIGNED.getOrDefault(skill, 0);
    }

    public static int available() {
        return available;
    }

    public static boolean isAvailableKnown() {
        return availableKnown && isCurrentCharacter();
    }

    public static long updatedAt() {
        return updatedAt;
    }

    public static void updateFromCurrentMenu() {
        if (!WynncraftMenuService.isCurrent(MenuType.CHARACTER_INFO)) return;
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (!(client.currentScreen instanceof HandledScreen<?> screen)) return;
            if (screen.getScreenHandler().slots.size() < 16) return;

            EnumMap<SkillPoint, Integer> parsed = new EnumMap<>(SkillPoint.class);
            Integer parsedAvailable = null;
            boolean found = false;
            for (int i = 0; i < 5; i++) {
                ItemStack stack = screen.getScreenHandler().slots.get(11 + i).getStack();
                String text = stackText(stack);
                Integer value = parseAssigned(text, SkillPoint.values()[i]);
                if (value != null) {
                    parsed.put(SkillPoint.values()[i], value);
                    found = true;
                }
            }
            if (!found) return;
            if (client.player != null) {
                Matcher availableMatcher = AVAILABLE.matcher(stackText(client.player.getInventory().getStack(7)));
                if (availableMatcher.find()) {
                    parsedAvailable = safeInt(availableMatcher.group(1));
                }
            }
            if (parsedAvailable == null) {
                for (Slot slot : screen.getScreenHandler().slots) {
                    Matcher availableMatcher = AVAILABLE.matcher(stackText(slot.getStack()));
                    if (availableMatcher.find()) {
                        parsedAvailable = safeInt(availableMatcher.group(1));
                        break;
                    }
                }
            }
            ASSIGNED.clear();
            ASSIGNED.putAll(parsed);
            availableKnown = parsedAvailable != null;
            available = parsedAvailable == null ? 0 : parsedAvailable;
            sourceCharacterId = CharacterState.id().orElse(null);
            updatedAt = System.currentTimeMillis();
        } catch (Throwable ignored) {}
    }

    private static boolean isCurrentCharacter() {
        return sourceCharacterId != null && Objects.equals(sourceCharacterId, CharacterState.id().orElse(null));
    }

    static Integer parseAssigned(String text, SkillPoint skill) {
        if (text == null || skill == null) return null;
        String label = skill == SkillPoint.DEFENCE ? "defen[cs]e" : skill.name().toLowerCase();
        Matcher matcher = Pattern.compile("(?i)" + label + "\\D{0,12}(\\d{1,3})").matcher(text);
        if (matcher.find()) return safeInt(matcher.group(1));
        matcher = NUMBER.matcher(text);
        return matcher.find() ? safeInt(matcher.group(1)) : null;
    }

    private static String stackText(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        StringBuilder text = new StringBuilder(stack.getName().getString());
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) for (Text line : lore.lines()) text.append('\n').append(line.getString());
        return text.toString().replaceAll("§[0-9a-fk-or]", "");
    }

    private static int safeInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
